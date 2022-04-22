package services;

import storage.EventStorage;

public abstract class EventHandlingService {
    protected final EventStorage storage;

    public EventHandlingService(EventStorage storage) {
        this.storage = storage;
    }
}
