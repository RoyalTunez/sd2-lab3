package storage;

import events.Event;

public interface StatSubscriber {
    void process(Event event);
}
