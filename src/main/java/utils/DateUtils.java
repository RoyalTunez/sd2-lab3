package utils;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtils {
    public static Date truncateToDays(Date date) {
        return new Date(date.toInstant().truncatedTo(ChronoUnit.DAYS).toEpochMilli());
    }
}
