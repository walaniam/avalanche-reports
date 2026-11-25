package walaniam.avalanches.common.logging;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoggingUtilsTest {

    @Test
    void logsExceptionDetailsAndStackTraceThroughFunctionLogger() {
        ExecutionContext context = mock(ExecutionContext.class);
        Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        when(context.getFunctionName()).thenReturn("ingestReportOnDemand");
        var exception = new IllegalStateException("write failed");

        LoggingUtils.logWarn(context, "Report ingestion failed", exception);

        verify(logger).log(Level.WARNING,
            "[ingestReportOnDemand] Report ingestion failed: java.lang.IllegalStateException: write failed",
            exception);
    }

    @Test
    void logsWarningWithoutException() {
        ExecutionContext context = mock(ExecutionContext.class);
        Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        when(context.getFunctionName()).thenReturn("ingestReportOnDemand");

        LoggingUtils.logWarn(context, "Report missing", null);

        verify(logger).log(Level.WARNING, "[ingestReportOnDemand] Report missing");
    }
}
