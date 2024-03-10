package walaniam.avalanches.common.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilsTest {

    @Test
    void shouldParseEuropeWarsawZonedDate() {
        ZonedDateTime dateTime = DateTimeUtils.parseDate("2024-03-09 18:01", ZoneId.of("Europe/Warsaw"));
        assertThat(dateTime.toString()).isEqualTo("2024-03-09T18:01+01:00[Europe/Warsaw]");
    }

    @Test
    void shouldParseEuropeWarsawZonedDateAndConvertToUtc() {
        ZonedDateTime dateTime = DateTimeUtils.parseDate("2024-03-09 18:01", ZoneId.of("Europe/Warsaw"));
        LocalDateTime utc = DateTimeUtils.toUtc(dateTime);
        assertThat(utc.toString()).isEqualTo("2024-03-09T17:01");
    }
}