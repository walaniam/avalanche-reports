package walaniam.avalanches;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import walaniam.avalanches.function.AvalancheReportsFunctionsHandler;
import walaniam.avalanches.mongo.BinaryReportMongoRepository;
import walaniam.avalanches.persistence.BinaryReport;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

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

        var binaryRepository = new BinaryReportMongoRepository(
            executionContext,
            MONGO_DB_CONTAINER.getConnectionString()
        );

        List<BinaryReport> allLatest = binaryRepository.getLatest(10);
        Assertions.assertEquals(1, allLatest.size());

        Optional<BinaryReport> todayPdfReport = binaryRepository.findByDay(allLatest.stream().findFirst().get().getDay());
        Assertions.assertTrue(todayPdfReport.isPresent());
    }
}
