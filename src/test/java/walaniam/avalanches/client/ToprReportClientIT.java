package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.ReportId;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ToprReportClientIT {

    private final ExecutionContext executionContext = mock(ExecutionContext.class);

    private ToprReportClient underTest;

    @BeforeEach
    public void beforeEach() {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();
        underTest = new ToprReportClient();
    }

    @Test
    void shouldFetchCurrentReport() throws ReportFetchException {
        ReportFetchResult fetchResult = underTest.fetch(executionContext);
        AvalancheReport report = fetchResult.getReport();
        assertThat(report).extracting(AvalancheReport::getAvalancheLevel).isIn(1, 2, 3, 4, 5);
        assertThat(report).extracting(AvalancheReport::getReportDate).isNotNull();
        assertThat(report).extracting(AvalancheReport::getReportExpirationDate).isNotNull();
        assertThat(report.getReportExpirationDate()).isAfter(report.getReportDate());
        assertThat(report).extracting(AvalancheReport::getComment).asString().isNotBlank();
        assertThat(report).extracting(AvalancheReport::getId).isNotNull();
        assertThat(report)
            .extracting(AvalancheReport::getId)
            .extracting(ReportId::getReportedBy)
            .isEqualTo("topr");
        assertThat(report.getId().getReportDate()).isEqualTo(report.getReportDate());

        BinaryReport binaryReport = fetchResult.getBinaryReport();
        assertThat(binaryReport).extracting(BinaryReport::getId).isEqualTo(report.getId());
        assertThat(binaryReport).extracting(BinaryReport::getBytes).isNotNull();
        assertThat(binaryReport.getDay()).isEqualTo(report.getId().getReportDate().toLocalDate());
    }
}