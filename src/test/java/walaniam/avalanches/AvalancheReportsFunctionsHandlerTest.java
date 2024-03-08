package walaniam.avalanches;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.stubbing.Answer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import walaniam.avalanches.function.AvalancheReportsFunctionsHandler;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Testcontainers
class AvalancheReportsFunctionsHandlerTest {

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

    private static void mockResponseBuilderOf(HttpRequestMessage requestMessage) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(requestMessage).createResponseBuilder(any(HttpStatus.class));
    }
}
