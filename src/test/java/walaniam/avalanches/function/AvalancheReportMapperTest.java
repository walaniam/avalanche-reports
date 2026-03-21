package walaniam.avalanches.function;

import org.junit.jupiter.api.Test;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AvalancheReportMapperTest {

    @Test
    void toDataView() {
        AvalancheReport dto = AvalancheReport.builder()
            .id(ReportId.builder()
                .reportDate(LocalDateTime.now())
                .regionId("PL-12")
                .reportedBy("TOPR_reporter")
                .build())
            .build();

        AvalancheReportDto reportDto = AvalancheReportMapper.INSTANCE.toDataView(dto);
        assertThat(reportDto.getReportedBy()).isEqualTo("TOPR_reporter");
        assertThat(reportDto.getRegion()).isNotNull();
        assertThat(reportDto.getRegion().getId()).isEqualTo("PL-12");
        assertThat(reportDto.getRegion().getName()).isNotNull();
    }
}