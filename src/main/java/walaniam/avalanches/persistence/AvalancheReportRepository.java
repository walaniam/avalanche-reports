package walaniam.avalanches.persistence;

import java.util.List;

public interface AvalancheReportRepository {

    void save(AvalancheReport report);
    List<AvalancheReport> getLatest(int limit);

    default List<AvalancheReport> getLatest() {
        return getLatest(10);
    }
}
