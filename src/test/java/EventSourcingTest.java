import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import services.EnterService;
import services.ManagerService;
import services.StatService;
import storage.EventStorage;
import utils.DateUtils;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

public class EventSourcingTest {
    private ManagerService managerService;
    private EnterService enterService;
    private StatService statService;
    private Date now;

    @BeforeEach
    void initTest() {
        EventStorage storage = new EventStorage();
        managerService = new ManagerService(storage);
        enterService = new EnterService(storage);
        statService = new StatService(storage);
        now = new Date();
    }

    Date fromNow(int seconds) {
        return new Date(now.getTime() + seconds * 1000L);
    }

    @Test
    void usualEnterTest() {
        int id = managerService.registerMembership(now, fromNow(2));
        assertTrue(enterService.enter(id, fromNow(1)));
    }

    @Test
    void expiredEnterTest() {
        int id = managerService.registerMembership(now, fromNow(2));
        assertFalse(enterService.enter(id, fromNow(5)));
    }

    @Test
    void doubleEnterTest() {
        int id = managerService.registerMembership(now, fromNow(5));
        assertTrue(enterService.enter(id, fromNow(2)));
        assertFalse(enterService.enter(id, fromNow(3)));
    }

    @Test
    void enterExitTest() {
        int id = managerService.registerMembership(now, fromNow(10));
        assertTrue(enterService.enter(id, fromNow(1)));
        assertTrue(enterService.exit(id, fromNow(2)));
        assertTrue(enterService.enter(id, fromNow(3)));
        assertTrue(enterService.exit(id, fromNow(4)));
    }

    @Test
    void exitWithoutEnterTest() {
        int id = managerService.registerMembership(now, fromNow(10));
        assertFalse(enterService.exit(id, fromNow(1)));
    }

    @Test
    void doubleExitAfterEnterTest() {
        int id = managerService.registerMembership(now, fromNow(10));
        assertTrue(enterService.enter(id, fromNow(1)));
        assertTrue(enterService.exit(id, fromNow(2)));
        assertFalse(enterService.exit(id, fromNow(3)));
    }

    @Test
    void extendMembershipTest() {
        int id = managerService.registerMembership(now, fromNow(10));
        assertTrue(enterService.enter(id, fromNow(1)));
        assertTrue(enterService.exit(id, fromNow(2)));
        assertFalse(enterService.enter(id, fromNow(15)));
        managerService.extendMembership(id, Duration.ofMillis(10_000));
        assertTrue(enterService.enter(id, fromNow(19)));
        assertFalse(enterService.enter(id, fromNow(21)));
    }

    @Test
    void getMembershipInfoTest() {
        int expire = 10, firstExtend = 11, secondExtend = 25;
        int id = managerService.registerMembership(now, fromNow(expire));

        var info = managerService.getMembershipInfo(id);
        assertEquals(now, info.getRegisterDate());
        assertEquals(fromNow(expire), info.getExpireDate());
        assertFalse(info.isEntered());
        assertEquals(0, info.getAttendanceCount());

        assertTrue(enterService.enter(id, fromNow(expire - 5)));
        assertTrue(enterService.exit(id, fromNow(expire + 5)));

        managerService.extendMembership(id, Duration.ofMillis(firstExtend * 1000L));
        expire += firstExtend;
        assertTrue(enterService.enter(id, fromNow(expire - 2)));
        assertTrue(enterService.exit(id, fromNow(expire + 2)));

        managerService.extendMembership(id, Duration.ofMillis(secondExtend * 1000L));
        expire += secondExtend;
        assertTrue(enterService.enter(id, fromNow(expire - 10)));
        assertTrue(enterService.exit(id, fromNow(expire - 9)));
        assertTrue(enterService.enter(id, fromNow(  expire - 8)));

        info = managerService.getMembershipInfo(id);
        assertEquals(now, info.getRegisterDate());
        assertEquals(fromNow(expire), info.getExpireDate());
        assertTrue(info.isEntered());
        assertEquals(4, info.getAttendanceCount());
    }

    @Test
    void truncateTest() {
        assertEquals(new Date(0), DateUtils.truncateToDays(new Date(1000)));
        long nextDay = 1000 * 60 * 60 * 24;
        assertEquals(new Date(nextDay), DateUtils.truncateToDays(new Date(nextDay + 10)));
        assertEquals(new Date(nextDay), DateUtils.truncateToDays(new Date(nextDay + 1000)));
        assertEquals(new Date(nextDay), DateUtils.truncateToDays(new Date(nextDay + 1000 * 60 * 60 * 24 - 1)));
    }

    @Test
    void statsTest() {
        Random random = new Random(new Date().getTime());
        int memberCount = 100;
        int attendanceCount = 1000;
        int wholeDaysCount = 9;
        int secondsInDay = 24 * 60 * 60;
        int daysSeconds = wholeDaysCount * secondsInDay;
        int minDuration = 60 * 60;
        int maxDurationAdd = 60 * 60 * 5;
        int[] ids = new int[memberCount];
        Map<Date, Integer> durations = new HashMap<>();
        Map<Date, Integer> attendances = new HashMap<>();
        int totalDuration = 0;
        for (int i = 0; i < memberCount; i++) {
            ids[i] = managerService.registerMembership(now, fromNow(daysSeconds));
        }

        for (int i = 0; i < attendanceCount; i++) {
            int membershipId = ids[random.nextInt(memberCount)];
            int enterInt = random.nextInt(daysSeconds);
            int duration = random.nextInt(maxDurationAdd) + minDuration;
            Date enterDate = fromNow(enterInt);
            Date exitDate = fromNow(enterInt + duration);
            assertTrue(enterService.enter(membershipId, enterDate));
            assertTrue(enterService.exit(membershipId, exitDate));
            totalDuration += duration;

            Date key = DateUtils.truncateToDays(enterDate);
            durations.putIfAbsent(key, 0);
            attendances.putIfAbsent(key, 0);
            durations.computeIfPresent(key, (date, dur) -> dur + duration);
            attendances.computeIfPresent(key, (date, att) -> att + 1);
        }

        attendances.forEach(((date, attendance) -> {
            int expectedDuration = durations.getOrDefault(date, -1);
            assertNotEquals(-1, expectedDuration);
            var stat = statService.getStatisticsOfDate(date);
            assertNotNull(stat);
            assertEquals(attendance, stat.getAttendance());
            assertEquals(expectedDuration / attendance, stat.averageDuration().toMillis() / 1000);
        }));
        var avgDuration = statService.getAverageDuration();
        assertEquals(totalDuration / attendanceCount, avgDuration.toMillis() / 1000);
        assertEquals(attendanceCount / attendances.size(), statService.getAverageAttendance());
    }

    @Test
    void parallelStatsTest() {
        Random random = new Random(new Date().getTime());
        int memberCount = 1000;
        int attendanceCount = 20000;
        int wholeDaysCount = 9;
        int secondsInDay = 24 * 60 * 60;
        int daysSeconds = wholeDaysCount * secondsInDay;
        int minDuration = 60 * 60;
        int maxDurationAdd = 60 * 60 * 5;
        int[] ids = new int[memberCount];
        Map<Date, Integer> durations = new ConcurrentHashMap<>();
        Map<Date, Integer> attendances = new ConcurrentHashMap<>();
        AtomicReferenceArray<ReentrantLock> mutices = new AtomicReferenceArray<>(memberCount);
        range(0, memberCount).forEach(i -> mutices.set(i, new ReentrantLock()));
        final AtomicInteger totalDuration = new AtomicInteger(0);
        range(0, memberCount).parallel().forEach(i -> ids[i] = managerService.registerMembership(now, fromNow(daysSeconds)));

        range(0, attendanceCount).parallel().forEach(i -> {
            int membershipId = ids[random.nextInt(memberCount)];
            int enterInt = random.nextInt(daysSeconds);
            int duration = random.nextInt(maxDurationAdd) + minDuration;
            Date enterDate = fromNow(enterInt);
            Date exitDate = fromNow(enterInt + duration);
            mutices.get(membershipId).lock();
            enterService.enter(membershipId, enterDate);
            enterService.exit(membershipId, exitDate);
            mutices.get(membershipId).unlock();
            totalDuration.addAndGet(duration);
            Date key = DateUtils.truncateToDays(enterDate);
            durations.putIfAbsent(key, 0);
            attendances.putIfAbsent(key, 0);
            durations.computeIfPresent(key, (date, dur) -> dur + duration);
            attendances.computeIfPresent(key, (date, att) -> att + 1);
        });

        attendances.forEach(((date, attendance) -> {
            int expectedDuration = durations.getOrDefault(date, -1);
            assertNotEquals(-1, expectedDuration);
            var stat = statService.getStatisticsOfDate(date);
            assertNotNull(stat);
            assertEquals(attendance, stat.getAttendance());
            assertEquals(expectedDuration / attendance, stat.averageDuration().toMillis() / 1000);
        }));
        var avgDuration = statService.getAverageDuration();
        assertEquals(totalDuration.get() / attendanceCount, avgDuration.toMillis() / 1000);
        assertEquals(attendanceCount / attendances.size(), statService.getAverageAttendance());
    }
}
