package walaniam.avalanches.common.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTimeUtils {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ZonedDateTime parseDate(String dateTime, ZoneId atZone) {
        var localDateTime = LocalDateTime.parse(dateTime, DT_FORMATTER);
        return ZonedDateTime.of(localDateTime, atZone);
    }

    public static LocalDateTime toUtc(ZonedDateTime dateTime) {
        return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC);
    }

    public static String formatAtUtc(ZonedDateTime dateTime) {
        LocalDateTime localDateTime = toUtc(dateTime);
        return localDateTime.format(DT_FORMATTER);
    }
}
