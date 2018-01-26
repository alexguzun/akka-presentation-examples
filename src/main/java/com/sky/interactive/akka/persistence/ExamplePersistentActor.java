package com.sky.interactive.akka.persistence;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;

class ExamplePersistentActor extends AbstractPersistentActor {
    private final LoggingAdapter log = Logging.getLogger(this);

    private final String persistenceId;
    private ExampleState state = new ExampleState();
    private int snapShotInterval = 100;

    public ExamplePersistentActor(String persistenceId) {
        this.persistenceId = persistenceId;
    }

    private int getNumEvents() {
        return state.size();
    }

    @Override
    public String persistenceId() {
        return persistenceId;
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Evt.class, evt -> {
                    log.info("Received event {}", evt.getData());
                    this.state.update(evt);
                })
                .match(SnapshotOffer.class, ss -> {
                    state = (ExampleState) ss.snapshot();
                    log.info("Received snapshot with {} events", state.size());
                })
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Cmd.class, c -> {
                    final String data = c.getData();
                    final Evt evt = new Evt(data + "-" + getNumEvents());
                    persist(evt, (Evt e) -> {
                        state.update(e);

                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0)
                            // Create a copy of snapshot because ExampleState is mutable
                            saveSnapshot(state.copy());
                    });
                })
                .matchEquals("get", s -> sender().tell(state.copy(), getSelf()))
                .build();
    }
}