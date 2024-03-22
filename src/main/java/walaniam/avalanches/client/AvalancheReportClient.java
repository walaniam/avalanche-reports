package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;

public interface AvalancheReportClient {

    ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException;
}
