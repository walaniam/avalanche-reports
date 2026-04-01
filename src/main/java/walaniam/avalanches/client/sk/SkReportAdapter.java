package walaniam.avalanches.client.sk;

import com.microsoft.azure.functions.ExecutionContext;
import org.apache.commons.lang3.StringUtils;
import walaniam.avalanches.client.api.AvalancheReportClient;
import walaniam.avalanches.client.api.ReportFetchException;
import walaniam.avalanches.client.api.ReportFetchResult;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.ReportId;
import walaniam.avalanches.regions.Region;
import walaniam.avalanches.regions.RegionsMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

/**
 * {@link AvalancheReportClient} implementation that fetches Slovak avalanche bulletins
 * from {@code https://static.laviny.sk/bulletins/latest/en.xml} (CAAMLv5 BulletinEAWS format)
 * and adapts them to the common {@link ReportFetchResult}/{@link AvalancheReport} model.
 */
public class SkReportAdapter implements AvalancheReportClient {

    static final String BINARY_REPORT_URL = "https://static.laviny.sk/bulletins/{DATE}/{DATE}_SK_en.pdf";
    static final String REPORTER = "sk-laviny";

    private final SkReportClient skReportClient;
    private final String binaryReportUrlTemplate;
    private final Set<Region> regions;

    public SkReportAdapter() {
        this(new SkReportClient(), BINARY_REPORT_URL);
    }

    SkReportAdapter(SkReportClient skReportClient, String binaryReportUrlTemplate) {
        this.skReportClient = skReportClient;
        this.binaryReportUrlTemplate = binaryReportUrlTemplate;
        this.regions = new RegionsMapper().getAll().stream()
            .filter(region -> StringUtils.startsWithIgnoreCase(region.getId(), "SK-"))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Region> getSupportedRegions() {
        return regions;
    }

    @Override
    public ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException {
        logInfo(executionContext, "Fetching SK report from %s", SkReportClient.REPORT_URL);
        try {
            List<AvalancheReport> allReports = new ArrayList<>();
            for (SkBulletin bulletin : skReportClient.fetch()) {
                allReports.addAll(adapt(bulletin));
            }

            var fetchResultBuilder = ReportFetchResult.builder()
                .reports(allReports);

            if (!allReports.isEmpty()) {
                AvalancheReport firstReport = allReports.get(0);
                String binaryUrl = resolveBinaryReportUrl(firstReport.getReportExpirationDate());
                try {
                    var id = ReportId.builder()
                        .reportedBy(REPORTER)
                        .regionId("SK")
                        .reportDate(firstReport.getReportDate())
                        .build();
                    BinaryReport binaryReport = fetchBinaryReport(executionContext, binaryUrl)
                        .id(id)
                        .day(firstReport.getReportExpirationDate().toLocalDate())
                        .build();
                    fetchResultBuilder.binaryReport(binaryReport);
                } catch (Exception e) {
                    logWarn(executionContext, "Failed to fetch SK binary report", e);
                }
            }

            return fetchResultBuilder.build();

        } catch (Exception e) {
            logWarn(executionContext, "Failed to fetch SK report", e);
            throw new ReportFetchException("Failed to fetch SK avalanche report", e);
        }
    }

    private BinaryReport.BinaryReportBuilder fetchBinaryReport(ExecutionContext executionContext, String url) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(url))
            .build();

        logInfo(executionContext, "Fetching binary report from %s", url);

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        logInfo(executionContext, "Response from %s returned status %s", url, response.statusCode());

        if (response.statusCode() / 100 == 2) {
            return BinaryReport.builder()
                .bytes(response.body())
                .contentType("application/pdf");
        } else {
            throw new RuntimeException("Got status: " + response.statusCode());
        }
    }

    private String resolveBinaryReportUrl(LocalDateTime reportExpirationDate) {
        String date = reportExpirationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return binaryReportUrlTemplate.replace("{DATE}", date);
    }

    List<AvalancheReport> adapt(SkBulletin bulletin) {
        LocalDateTime beginDate = toUtc(bulletin.getBeginPosition());
        return bulletin.getRegionIds().stream()
            .map(regionId -> {
                var id = ReportId.builder()
                    .reportedBy(REPORTER)
                    .regionId(regionId)
                    .reportDate(beginDate)
                    .build();

                return AvalancheReport.builder()
                    .id(id)
                    .avalancheLevel(bulletin.getMaxDangerRating())
                    .reportDate(beginDate)
                    .reportExpirationDate(toUtc(bulletin.getEndPosition()))
                    .comment(prepareComment(bulletin))
                    .build();
            })
            .toList();
    }

    private static String prepareComment(SkBulletin bulletin) {
        return String.format("Highlights: %s\nComment: %s\nSnow: %s",
            bulletin.getHighlights(), bulletin.getComment(), bulletin.getSnowpackStructureComment());
    }

    private static LocalDateTime toUtc(LocalDateTime utcNaive) {
        if (utcNaive == null) {
            return null;
        }
        // The XML timestamps are already expressed in UTC (Z suffix), parsed as plain LocalDateTime.
        // Re-express at UTC offset to be consistent with the rest of the domain model.
        return utcNaive.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}

