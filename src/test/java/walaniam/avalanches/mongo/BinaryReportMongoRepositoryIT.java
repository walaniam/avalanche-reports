package walaniam.avalanches.mongo;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.ReportId;

import java.time.LocalDate;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class BinaryReportMongoRepositoryIT {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
        DockerImageName.parse("mongo").withTag("4.2.22")
    ).withReplicaSet();

    @Test
    void upsertsByReportIdAndPreservesOtherReports() {
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getGlobal());
        var repository = new BinaryReportMongoRepository(context, MONGO_DB_CONTAINER.getConnectionString());
        var day = LocalDate.of(2026, 11, 25);
        var report = BinaryReport.builder()
            .id(new ReportId(day.atTime(16, 14), "topr"))
            .day(day)
            .contentType("application/pdf")
            .bytes(new byte[]{1, 2})
            .build();
        var otherReport = BinaryReport.builder()
            .id(new ReportId(day.plusDays(1).atTime(16, 14), "topr"))
            .day(day.plusDays(1))
            .contentType("application/pdf")
            .bytes(new byte[]{3})
            .build();

        repository.upsert(report);
        repository.upsert(otherReport);
        report.setBytes(new byte[]{4, 5, 6});
        repository.upsert(report);
        repository.upsert(report);

        assertThat(repository.getLatest(10)).hasSize(2);
        assertThat(repository.findByDay(day)).contains(report);
        assertThat(repository.findByDay(day.plusDays(1))).contains(otherReport);
    }
}
