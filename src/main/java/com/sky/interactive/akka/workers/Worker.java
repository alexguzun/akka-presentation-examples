package com.sky.interactive.akka.workers;

import akka.actor.AbstractLoggingActor;

public class Worker extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> {
                    Thread.sleep(100);
                    sender().tell("done", self());
                    log().info("Sent message {}", o);
                })
                .build();
    }
}
