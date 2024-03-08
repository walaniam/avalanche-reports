package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.persistence.AvalancheReport;

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
    void fetch() throws ReportFetchException {
        AvalancheReport report = underTest.fetch(executionContext);
        assertThat(report).extracting(AvalancheReport::getAvalancheLevel).isIn(1, 2, 3, 4, 5);
    }
}