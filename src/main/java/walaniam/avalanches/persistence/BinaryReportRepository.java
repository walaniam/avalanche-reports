package walaniam.avalanches.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BinaryReportRepository {

    void upsert(BinaryReport document);
    Optional<BinaryReport> findByDay(LocalDate day);
    List<BinaryReport> getLatest(int limit);
}
