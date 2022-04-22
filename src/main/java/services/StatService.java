package services;

import events.Event;
import events.MemberEntered;
import events.MemberLeft;
import storage.EventStorage;
import storage.StatSubscriber;
import utils.DateUtils;
import utils.DynamicArray;
import utils.LockFreeDynamicArray;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class StatService extends EventHandlingService implements StatSubscriber {
    public static class Statistics {
        private final AtomicReference<Duration> duration = new AtomicReference<>(Duration.ZERO);
        private final AtomicInteger attendanceCount = new AtomicInteger(0);

        public Duration averageDuration() {
            return duration.get().dividedBy(attendanceCount.get());
        }

        public Statistics addAttendance(Duration duration) {
            attendanceCount.incrementAndGet();
            Duration prev;

            do {
                prev = this.duration.get();
            } while (!this.duration.compareAndSet(prev, prev.plus(duration)));

            return this;
        }

        public int getAttendance() {
            return attendanceCount.get();
        }
    }

    private final DynamicArray<Date> lastEnter = new LockFreeDynamicArray<>();
    private final Map<Date, Statistics> stats = new ConcurrentHashMap<>();
    private final Statistics totalStats = new Statistics();

    public StatService(EventStorage storage) {
        super(storage);
        storage.subscribe(this);
    }

    @Override
    public void process(Event event) {
        var membershipId = event.getMembershipId();

        if (event instanceof MemberEntered) {
            ensureMembershipId(membershipId);
            lastEnter.put(membershipId, ((MemberEntered) event).getEnterDate());
        } else if (event instanceof MemberLeft) {
            ensureMembershipId(membershipId);
            Date enter = lastEnter.get(membershipId);

            if (enter != null) {
                Date enterDay = DateUtils.truncateToDays(enter);
                Date exit = ((MemberLeft) event).getExitDate();
                Duration duration = Duration.between(enter.toInstant(), exit.toInstant());

                stats.putIfAbsent(enterDay, new Statistics());
                stats.computeIfPresent(enterDay, (date, statistics) -> statistics.addAttendance(duration));
                totalStats.addAttendance(duration);
                lastEnter.put(membershipId, null);
            }
        }
    }

    public Statistics getStatisticsOfDate(Date date) {
        return stats.getOrDefault(date, null);
    }

    public Duration getAverageDuration() {
        return totalStats.averageDuration();
    }

    public int getAverageAttendance() {
        return totalStats.attendanceCount.get() / stats.size();
    }

    public void ensureMembershipId(int membershipId) {
        while (lastEnter.getSize() <= membershipId) {
            lastEnter.pushBack(null);
        }
    }
}
