package walaniam.avalanches.persistence;

import java.util.List;

public interface AvalancheReportRepository {

    void save(AvalancheReport report);

    List<AvalancheReport> getLatest(int skip, int limit);

    default List<AvalancheReport> getLatest() {
        return getLatest(0, 10);
    }
}
