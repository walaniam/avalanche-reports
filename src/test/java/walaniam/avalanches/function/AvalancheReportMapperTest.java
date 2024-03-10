package walaniam.avalanches.function;

import org.junit.jupiter.api.Test;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvalancheReportMapperTest {

    @Test
    void toDataView() {
        AvalancheReport dto = AvalancheReport.builder()
            .id(ReportId.builder()
                .reportDate(LocalDateTime.now())
                .reportedBy("TOPR_reporter")
                .build())
            .build();

        AvalancheReportDto reportDto = AvalancheReportMapper.INSTANCE.toDataView(dto);
        assertEquals("TOPR_reporter", reportDto.getReportedBy());
    }
}