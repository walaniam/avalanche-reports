package walaniam.avalanches.common.time;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateTimeUtils {

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

    public static LocalDateTime fromUtcString(String dateTime) {
        return LocalDateTime.parse(dateTime, DT_FORMATTER);
    }

    public static String toUtcString(LocalDateTime time) {
        return time.format(DT_FORMATTER);
    }
}
