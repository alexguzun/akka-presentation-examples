package com.sky.interactive.akka.persistence;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import ch.qos.logback.core.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExamplePersistentActorTest {
    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
        try {
            FileUtils.forceDelete(new File("journal"));
            FileUtils.forceDelete(new File("snapshots"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIt() {
        new TestKit(system) {{
            String id = UUID.randomUUID().toString();
            final Props props = Props.create(ExamplePersistentActor.class, id);
            final ActorRef persistenceActor = system.actorOf(props, "persistence-" + id);

            List<String> data = new ArrayList<>();

            for (int i = 0; i < 200; i++) {
                data.add(RandomStringUtils.randomAlphanumeric(5));
            }

            data.forEach(d -> persistenceActor.tell(new Cmd(d), getRef()));

            expectNoMsg(duration("3 seconds"));

            persistenceActor.tell("get", getRef());

            ExampleState exampleState = expectMsgClass(ExampleState.class);

            Assert.assertEquals(exampleState.size(), data.size());
        }};
    }


    @Test
    public void testRecovery() {
        String id = UUID.randomUUID().toString();

        List<String> data = new ArrayList<>();

        for (int i = 0; i < 220; i++) {
            data.add(RandomStringUtils.randomAlphanumeric(5));
        }

        new TestKit(system) {{
            final Props props = Props.create(ExamplePersistentActor.class, id);
            final ActorRef persistenceActor = system.actorOf(props, "persistence-" + id);

            data.forEach(d -> persistenceActor.tell(new Cmd(d), getRef()));

            expectNoMsg(duration("3 seconds"));

            persistenceActor.tell("get", getRef());

            ExampleState exampleState = expectMsgClass(ExampleState.class);

            Assert.assertEquals(exampleState.size(), data.size());

            //Kill first actor
            persistenceActor.tell(PoisonPill.getInstance(), getRef());
            expectNoMsg(duration("1 seconds"));

            final ActorRef recoveredActor = system.actorOf(props, "recovered-" + id);

            recoveredActor.tell("get", getRef());

            ExampleState recoveredState = expectMsgClass(ExampleState.class);

            Assert.assertEquals(recoveredState.size(), data.size());
        }};
    }

}