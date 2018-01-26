package com.sky.interactive.akka.simple;

import akka.actor.AbstractLoggingActor;

public class SumActor extends AbstractLoggingActor {

    private int sum = 0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Integer.class,
                        val -> {
                            log().info("received value {}", val);
                            sum = sum + val;
                        })
                .matchEquals("sum", o -> {
                    sender().tell(this.sum, self());
                })
                .matchAny(o -> log().debug("received unknown message"))
                .build();
    }
}
