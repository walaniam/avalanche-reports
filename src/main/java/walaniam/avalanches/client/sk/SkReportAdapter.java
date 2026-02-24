package walaniam.avalanches.client.sk;

import com.microsoft.azure.functions.ExecutionContext;
import walaniam.avalanches.client.AvalancheReportClient;
import walaniam.avalanches.client.ReportFetchException;
import walaniam.avalanches.client.ReportFetchResult;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;
import walaniam.avalanches.regions.RegionsMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

/**
 * {@link AvalancheReportClient} implementation that fetches Slovak avalanche bulletins
 * from {@code https://static.laviny.sk/bulletins/latest/en.xml} (CAAMLv5 BulletinEAWS format)
 * and adapts them to the common {@link ReportFetchResult}/{@link AvalancheReport} model.
 */
public class SkReportAdapter implements AvalancheReportClient {

    static final String REPORTER = "sk-laviny";

    private final SkReportClient skReportClient;
    private final RegionsMapper regionsMapper = new RegionsMapper();

    public SkReportAdapter() {
        this(new SkReportClient());
    }

    SkReportAdapter(SkReportClient skReportClient) {
        this.skReportClient = skReportClient;
    }

    @Override
    public ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException {
        logInfo(executionContext, "Fetching SK report from %s", SkReportClient.REPORT_URL);
        try {
            List<AvalancheReport> allReports = new ArrayList<>();
            for (SkBulletin bulletin : skReportClient.fetch()) {
                allReports.addAll(adapt(bulletin));
            }

            return ReportFetchResult.builder()
                .reports(allReports)
                .build();

        } catch (Exception e) {
            logWarn(executionContext, "Failed to fetch SK report", e);
            throw new ReportFetchException("Failed to fetch SK avalanche report", e);
        }
    }

    List<AvalancheReport> adapt(SkBulletin bulletin) {
        LocalDateTime beginDate = toUtc(bulletin.getBeginPosition());
        return bulletin.getRegionIds().stream()
            .map(regionId -> {
                var id = ReportId.builder()
                    .reportedBy(REPORTER)
                    .regionName(regionsMapper.getRegionName(regionId).orElse(regionId))
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

