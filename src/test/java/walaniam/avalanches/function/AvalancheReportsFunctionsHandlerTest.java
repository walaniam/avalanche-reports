package walaniam.avalanches.function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AvalancheReportsFunctionsHandlerTest {

    @ParameterizedTest
    @CsvSource({
        "SK-01, SK",
        "SK-02, SK",
        "SK-08, SK",
        "SK, SK",
        "PL-12, PL-12",
    })
    void resolvePdfRegionShouldMapSkSubRegionsToSk(String input, String expected) {
        assertThat(AvalancheReportsFunctionsHandler.resolvePdfRegion(input)).isEqualTo(expected);
    }

    @Test
    void resolvePdfRegionShouldHandleNull() {
        assertThat(AvalancheReportsFunctionsHandler.resolvePdfRegion(null)).isNull();
    }
}
