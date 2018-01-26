package com.sky.interactive.akka.workers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

public class CoordinatorTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testIt() {
        new TestKit(system) {{
            final Props props = Props.create(Coordinator.class);
            final ActorRef coordinator = system.actorOf(props, "coordinator");

            Coordinator.SendEmailsCmd msg = new Coordinator.SendEmailsCmd();
            msg.content = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                msg.content.add(RandomStringUtils.randomAlphanumeric(20));
            }
            coordinator.tell(msg, getRef());

            coordinator.tell(RandomStringUtils.randomAlphanumeric(10), getRef());
            expectMsg(duration("1 seconds"), "busy");

            expectMsg(duration("4 seconds"), "done");
        }};
    }

}