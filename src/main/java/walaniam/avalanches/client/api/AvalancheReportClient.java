package walaniam.avalanches.client.api;

import com.microsoft.azure.functions.ExecutionContext;
import walaniam.avalanches.regions.Region;

import java.util.Set;

public interface AvalancheReportClient {

    ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException;

    Set<Region> getSupportedRegions();
}
