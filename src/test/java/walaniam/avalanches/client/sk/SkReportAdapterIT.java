package walaniam.avalanches.client.sk;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.client.ReportFetchException;
import walaniam.avalanches.client.ReportFetchResult;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SkReportAdapterIT {

    private final ExecutionContext executionContext = mock(ExecutionContext.class);

    private SkReportAdapter underTest;

    @BeforeEach
    void beforeEach() {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();
        underTest = new SkReportAdapter();
    }

    @Test
    void shouldFetchCurrentReport() throws ReportFetchException {
        ReportFetchResult fetchResult = underTest.fetch(executionContext);

        AvalancheReport report = fetchResult.getReport();
        assertThat(report).isNotNull();
        assertThat(report.getAvalancheLevel()).isIn(1, 2, 3, 4, 5);
        assertThat(report.getReportDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isAfter(report.getReportDate());
        assertThat(report.getComment()).isNotBlank();
        assertThat(report.getId()).isNotNull();
        assertThat(report.getId())
            .extracting(ReportId::getReportedBy)
            .isEqualTo("sk-laviny");
        assertThat(report.getId().getReportDate()).isEqualTo(report.getReportDate());

        assertThat(fetchResult.getBinaryReport()).isNotNull();
        assertThat(fetchResult.getBinaryReport().getBytes()).isNotEmpty();
    }
}
