package com.sky.interactive.akka.workers;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.japi.pf.FI.UnitApply;
import akka.routing.RoundRobinPool;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Coordinator extends AbstractLoggingActor {
    private final Scheduler scheduler = getContext().system().scheduler();
    private final ExecutionContextExecutor dispatcher = context().system().dispatcher();

    private static final int NR_WORKERS = 3;

    private final ActorRef router = getContext()
            .actorOf(
                    new RoundRobinPool(NR_WORKERS).props(Props.create(Worker.class)),
                    "router"
            );

    private final Receive readyBehaviour = receiveBuilder()
            .match(SendEmailsCmd.class, send())
            .matchAny(this::unhandled)
            .build();

    private final Receive busyBehaviour = receiveBuilder()
            .matchEquals("done", handleSent())
            .matchEquals("timeout", s -> {
                this.cmdSender.tell("timeout", self());
                context().unbecome();
            })
            .matchAny(o -> {
                log().info("Received message while doing super important work.");
                sender().tell("busy", self());
            })
            .build();

    private ActorRef cmdSender;
    private int pending;

    @Override
    public Receive createReceive() {
        return readyBehaviour;
    }

    private UnitApply<SendEmailsCmd> send() {
        return sendEmailsCmd -> {
            this.cmdSender = getSender();

            sendEmailsCmd.content
                    .forEach(msg -> router.tell(msg, getSelf()));

            FiniteDuration timeout = FiniteDuration.apply(1, TimeUnit.SECONDS);

            scheduler.scheduleOnce(timeout, self(), "timeout", dispatcher, self());

            this.pending = sendEmailsCmd.content.size();
            getContext().become(busyBehaviour);
        };
    }

    private UnitApply<String> handleSent() {
        return emailSent -> {
            pending = pending - 1;
            log().info("One sent. {} left", pending);
            if (pending == 0) {
                cmdSender.tell("done", self());
                context().unbecome();
            }
        };
    }

    public static class SendEmailsCmd implements Serializable {
        List<String> content;
    }
}
