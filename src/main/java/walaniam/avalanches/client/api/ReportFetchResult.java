package walaniam.avalanches.client.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;

import java.util.List;

@Builder
@Getter
@ToString
public class ReportFetchResult {
    private List<AvalancheReport> reports;
    private BinaryReport binaryReport;

    public AvalancheReport getReport() {
        return reports == null || reports.isEmpty() ? null : reports.get(0);
    }
}
