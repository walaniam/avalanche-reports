package walaniam.avalanches.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvalancheReportRepository {

    void save(AvalancheReport report);

    List<AvalancheReport> getLatest(int skip, int limit);

    List<AvalancheReport> getLatest(String region, int skip, int limit);

    default List<AvalancheReport> getLatest() {
        return getLatest(0, 10);
    }

    Optional<AvalancheReport> find(String region, LocalDate day);
}
