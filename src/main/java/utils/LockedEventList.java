package utils;

import events.Event;
import storage.StatSubscriber;

import java.util.List;

public class LockedEventList extends Locked<List<Event>> {
    public LockedEventList(List<Event> eventList) {
        super(eventList);
    }

    public void applySubscriber(StatSubscriber subscriber) {
        object.forEach(subscriber::process);
    }
}
