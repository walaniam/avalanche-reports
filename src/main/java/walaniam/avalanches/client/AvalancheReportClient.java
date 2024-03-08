package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import walaniam.avalanches.persistence.AvalancheReport;

public interface AvalancheReportClient {

    AvalancheReport fetch(ExecutionContext executionContext) throws ReportFetchException;
}
