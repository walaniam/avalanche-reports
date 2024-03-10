package walaniam.avalanches.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.mongodb.MongoException;
import lombok.RequiredArgsConstructor;
import walaniam.avalanches.client.AvalancheReportClient;
import walaniam.avalanches.client.ReportFetchException;
import walaniam.avalanches.client.ToprReportClient;
import walaniam.avalanches.mongo.AvalancheReportMongoRepository;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.AvalancheReportRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

@RequiredArgsConstructor
public class AvalancheReportsFunctionsHandler {

    private static final String DAILY_18_30 = "0 30 18 * * *";
    private static final String EVERY_2_MINS = "0 */2 * * * *";

    private final Function<ExecutionContext, AvalancheReportRepository> repositoryProvider;
    private final AvalancheReportClient reportClient = new ToprReportClient();

    @SuppressWarnings("unused")
    public AvalancheReportsFunctionsHandler() {
        this(Optional.ofNullable(System.getenv("CosmosDBConnectionString"))
            .orElseGet(() -> {
                String localEnvMongo = "mongodb://mongo:mongo@localhost/avalanches_local:27017?ssl=false&authSource=admin";
                System.out.println("Using LOCAL env connection: " + localEnvMongo);
                System.out.println();
                return localEnvMongo;
            }));
    }

    public AvalancheReportsFunctionsHandler(String connectionString) {
        this(context -> new AvalancheReportMongoRepository(context, connectionString));
    }

    @FunctionName("ingestReport")
    public void ingestReport(@TimerTrigger(name = "ingestReportTrigger", schedule = DAILY_18_30) String timerInfo,
                             ExecutionContext context) {
        logInfo(context, "ingestReport triggered {}", timerInfo);
        try {
            AvalancheReport report = reportClient.fetch(context);
            AvalancheReportRepository repository = repositoryProvider.apply(context);
            repository.save(report);
        } catch (ReportFetchException e) {
            logWarn(context, "Report fetch failed, timer: " + timerInfo, e);
        }
    }

    @FunctionName("ingestReportOnDemand")
    public void ingestReportOnDemand(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<String> request,
        ExecutionContext context
    ) {
        logInfo(context, "ingestReport triggered on demand {}", request);
        try {
//            var now = LocalDateTime.now();
            AvalancheReport report = reportClient.fetch(context);
//            report.getId().setReportDate(now);
//            report.setReportDate(now);
//            report.setReportExpirationDate(LocalDateTime.now().plusHours(18));
            AvalancheReportRepository repository = repositoryProvider.apply(context);
            repository.save(report);
        } catch (ReportFetchException e) {
            logWarn(context, "Report fetch failed", e);
        }
    }

    @FunctionName("reports")
    public HttpResponseMessage getLatest(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<String> request,
        ExecutionContext context) {

        logInfo(context, "Getting latest reports");

        AvalancheReportRepository repository = repositoryProvider.apply(context);
        try {
            List<AvalancheReportDto> latest = repository.getLatest(20).stream()
                .map(AvalancheReportMapper.INSTANCE::toDataView)
                .toList();
            HttpResponseMessage.Builder responseBuilder = responseBuilderOf(request, HttpStatus.OK, Optional.of(latest));
            responseBuilder.header("Content-Type", "application/json");
            return responseBuilder.build();
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    @FunctionName("report")
    public HttpResponseMessage getSingleLatest(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<String> request,
        ExecutionContext context) {

        logInfo(context, "Getting latest single report");

        AvalancheReportRepository repository = repositoryProvider.apply(context);
        try {
            AvalancheReportDto latest = repository.getLatest(1).stream()
                .map(AvalancheReportMapper.INSTANCE::toDataView)
                .findFirst()
                .orElseThrow();
            HttpResponseMessage.Builder responseBuilder = responseBuilderOf(request, HttpStatus.OK, Optional.of(latest));
            responseBuilder.header("Content-Type", "application/json");
            return responseBuilder.build();
        } catch (NoSuchElementException e) {
            return responseOf(request, HttpStatus.NOT_FOUND, Optional.empty());
        } catch (MongoException e) {
            logWarn(context, "read failed", e);
            return responseOf(request, HttpStatus.INTERNAL_SERVER_ERROR, Optional.of(String.valueOf(e)));
        }
    }

    private static <T> HttpResponseMessage.Builder responseBuilderOf(HttpRequestMessage<String> request,
                                                                     HttpStatus status,
                                                                     Optional<T> message) {
        HttpResponseMessage.Builder builder = request.createResponseBuilder(status);
        message.ifPresent(builder::body);
        return builder;
    }

    private static <T> HttpResponseMessage responseOf(HttpRequestMessage<String> request,
                                                      HttpStatus status,
                                                      Optional<T> message) {
        return responseBuilderOf(request, status, message).build();
    }
}
