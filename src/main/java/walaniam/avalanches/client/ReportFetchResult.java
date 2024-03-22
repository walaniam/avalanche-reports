package walaniam.avalanches.client;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;

@Builder
@Getter
@ToString
public class ReportFetchResult {
    private AvalancheReport report;
    private BinaryReport binaryReport;
}
