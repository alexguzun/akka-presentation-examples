package com.sky.interactive.akka.simple;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Example {
    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("test");
        ActorRef sumActor = system.actorOf(Props.create(SumActor.class));

        //fire and forget
        sumActor.tell(1, ActorRef.noSender());
        sumActor.tell(1, ActorRef.noSender());
        sumActor.tell(1, ActorRef.noSender());
        sumActor.tell(1, ActorRef.noSender());

        Future<Object> future = Patterns.ask(sumActor, "sum", Timeout.apply(1, TimeUnit.SECONDS));
        Integer sum = (Integer) Await.result(future, Duration.apply("1 second"));

        system.log().info("Sum is {}", sum);

        System.exit(0);
    }
}
