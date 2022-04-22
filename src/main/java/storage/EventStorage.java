package storage;

import events.Event;
import utils.DynamicArray;
import utils.LockFreeDynamicArray;
import utils.LockedEventList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EventStorage {
    DynamicArray<LockedEventList> storage = new LockFreeDynamicArray<>();
    List<StatSubscriber> subscribers = new ArrayList<>();

    public int registerEventList() {
        return storage.pushBack(new LockedEventList(new ArrayList<>()));
    }

    public void saveEvent(Event event) {
        var membershipId = event.getMembershipId();

        if (membershipId > storage.getSize()) {
            throw new IllegalArgumentException("Membership id doesn't exist");
        }

        storage.get(membershipId).transaction((history) -> history.add(event));
        applyEvent(event);
    }

    public void applyEvent(Event event) {
        subscribers.forEach((statSubscriber -> statSubscriber.process(event)));
    }

    public void subscribe(StatSubscriber subscriber) {
        for (int i = 0; i < storage.getSize(); i++) {
            storage.get(i).applySubscriber(subscriber);
        }

        subscribers.add(subscriber);
    }

    public <R> R transactionOn(int membershipId, Function<List<Event>, R> fun) {
        return storage.get(membershipId).transaction(fun);
    }
}
