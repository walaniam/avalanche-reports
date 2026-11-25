package walaniam.avalanches.function;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.client.AvalancheReportClient;
import walaniam.avalanches.client.ReportFetchResult;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.AvalancheReportRepository;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.BinaryReportRepository;

import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvalancheReportsFunctionsHandlerTest {

    private final ExecutionContext context = mock(ExecutionContext.class);
    private final AvalancheReportClient client = mock(AvalancheReportClient.class);
    private final AvalancheReportRepository reports = mock(AvalancheReportRepository.class);
    private final BinaryReportRepository binaries = mock(BinaryReportRepository.class);
    private final AvalancheReportsFunctionsHandler handler = new AvalancheReportsFunctionsHandler(
        ignored -> reports, ignored -> binaries, client);

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(Logger.getGlobal());
    }

    @Test
    void onDemandIngestionSavesStructuredReportAndPdf() throws Exception {
        var report = AvalancheReport.builder().build();
        var binary = BinaryReport.builder().build();
        when(client.fetch(context)).thenReturn(ReportFetchResult.builder()
            .report(report).binaryReport(binary).build());

        handler.ingestReportOnDemand(null, context);

        verify(reports).save(report);
        verify(binaries).upsert(binary);
    }

    @Test
    void onDemandIngestionSavesStructuredReportWithoutPdf() throws Exception {
        var report = AvalancheReport.builder().build();
        when(client.fetch(context)).thenReturn(ReportFetchResult.builder().report(report).build());

        handler.ingestReportOnDemand(null, context);

        verify(reports).save(report);
        verifyNoInteractions(binaries);
    }
}
