package walaniam.avalanches.function;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import walaniam.avalanches.client.api.AvalancheReportClient;
import walaniam.avalanches.mongo.AvalancheReportMongoRepository;
import walaniam.avalanches.mongo.BinaryReportMongoRepository;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


@Testcontainers
class AvalancheReportsFunctionsHandlerIT {

    @Container
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo").withTag("4.2.22")
    );

    private final ExecutionContext executionContext = mock(ExecutionContext.class);
    private AvalancheReportsFunctionsHandler underTest;

    @BeforeEach
    public void beforeEach() {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();
        underTest = new AvalancheReportsFunctionsHandler(MONGO_DB_CONTAINER.getConnectionString());
    }

    @Test
    void shouldFetchAndStoreReport() {

        underTest.ingestReport("timer info", executionContext);

        // Assert AvalancheReport documents were stored
        var reportRepository = new AvalancheReportMongoRepository(
            executionContext,
            MONGO_DB_CONTAINER.getConnectionString()
        );

        List<AvalancheReport> reports = reportRepository.getLatest(0, 10);
        assertThat(reports).isNotEmpty();
        assertThat(reports).allSatisfy(report -> {
            assertThat(report.getId()).isNotNull();
            assertThat(report.getId().getReportedBy()).isNotBlank();
            assertThat(report.getId().getReportDate()).isNotNull();
            assertThat(report.getAvalancheLevel()).isBetween(1, 5);
            assertThat(report.getReportDate()).isNotNull();
            assertThat(report.getReportExpirationDate()).isNotNull();
            assertThat(report.getComment()).isNotBlank();
        });

        // Assert BinaryReport documents were stored
        var binaryRepository = new BinaryReportMongoRepository(
            executionContext,
            MONGO_DB_CONTAINER.getConnectionString()
        );

        List<BinaryReport> allLatest = binaryRepository.getLatest(10);
        assertThat(allLatest).hasSize(2);
        assertThat(allLatest).allSatisfy(binaryReport -> {
            assertThat(binaryReport.getDay()).isNotNull();
            assertThat(binaryReport.getContentType()).isEqualTo("application/pdf");
            assertThat(binaryReport.getBytes()).isNotEmpty();
        });

        Set<String> regions = underTest.getReportClients().stream()
            .map(AvalancheReportClient::getSupportedRegions)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        assertThat(regions).isNotEmpty();

        List<BinaryReport> binaryReports = regions.stream()
            .map(region -> binaryRepository.find(region, allLatest.get(0).getDay()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        assertThat(binaryReports).hasSize(2);

        assertThat(binaryReports).allSatisfy(report -> {
            assertThat(report.getId()).isNotNull();
            assertThat(report.getId().getReportedBy()).isNotBlank();
            assertThat(report.getId().getReportDate()).isNotNull();
            assertThat(report.getDay()).isNotNull();
            assertThat(report.getContentType()).isEqualTo("application/pdf");
            assertThat(report.getBytes()).isNotEmpty();
        });

    }
}
