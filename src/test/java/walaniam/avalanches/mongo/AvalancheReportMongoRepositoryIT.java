package walaniam.avalanches.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.time.LocalDate;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class AvalancheReportMongoRepositoryIT {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
        DockerImageName.parse("mongo").withTag("4.2.22")
    ).withReplicaSet();

    @Test
    void savesRepeatedlyAndReturnsLatestReportsByExpirationDate() {
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getGlobal());
        var repository = new AvalancheReportMongoRepository(context, MONGO_DB_CONTAINER.getConnectionString());
        var day = LocalDate.of(2026, 11, 25);
        var latest = AvalancheReport.builder()
            .id(new ReportId(day.atTime(16, 14), "topr"))
            .reportDate(day.atTime(16, 14))
            .reportExpirationDate(day.plusDays(1).atTime(19, 0))
            .avalancheLevel(2)
            .build();
        var older = AvalancheReport.builder()
            .id(new ReportId(day.minusDays(1).atTime(16, 14), "topr"))
            .reportDate(day.minusDays(1).atTime(16, 14))
            .reportExpirationDate(day.atTime(19, 0))
            .avalancheLevel(1)
            .build();

        repository.save(latest);
        repository.save(older);
        assertThat(repository.getLatest(0, 1)).containsExactly(latest);

        latest.setAvalancheLevel(3);
        latest.setComment("Updated forecast");
        repository.save(latest);
        repository.save(latest);

        assertThat(repository.getLatest(0, 20)).containsExactly(latest, older);
        assertThat(repository.getLatest(1, 1)).containsExactly(older);
    }
}
