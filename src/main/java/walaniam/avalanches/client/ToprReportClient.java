package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import lombok.ToString;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import walaniam.avalanches.common.time.DateTimeUtils;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.BinaryReport;
import walaniam.avalanches.persistence.ReportId;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

public class ToprReportClient implements AvalancheReportClient {

    private static final String REPORTER = "topr";
    private final String reportUrl = "https://lawiny.topr.pl/";

    @Override
    public ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException {
        int errors = 0;
        Exception lastException;
        do {
            logInfo(executionContext, "Fetching report: %s", reportUrl);

            try (WebClient webClient = new WebClient()) {
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setJavaScriptEnabled(false);

                HtmlPage page = webClient.getPage(reportUrl);

                var pageWrapper = new PageWrapper(page);

                LocalDateTime reportDate = parseDate(pageWrapper.getReportDate());

                var id = ReportId.builder()
                    .reportedBy(REPORTER)
                    .reportDate(reportDate)
                    .build();

                var avalancheReport = AvalancheReport.builder()
                    .id(id)
                    .avalancheLevel(pageWrapper.getAvalancheLevel())
                    .reportDate(reportDate)
                    .reportExpirationDate(parseDate(pageWrapper.getReportExpirationDate()))
                    .comment(pageWrapper.getComment())
                    .build();

                var fetchResultBuilder = ReportFetchResult
                    .builder()
                    .report(avalancheReport);

                try {
                    BinaryReport binaryReport = fetchBinaryReport(executionContext, pageWrapper.getPdfReportUrl())
                        .id(id)
                        .day(avalancheReport.getReportExpirationDate().toLocalDate())
                        .build();
                    fetchResultBuilder.binaryReport(binaryReport);
                } catch (Exception e) {
                    logWarn(executionContext, "Failed to fetch binary report", e);
                }

                return fetchResultBuilder.build();

            } catch (IOException e) {
                logWarn(executionContext, "Failed", e);
                errors++;
                lastException = e;
            }
        } while (errors < 2);

        throw new ReportFetchException("Fetch failed after attempts: " + errors, lastException);
    }

    private BinaryReport.BinaryReportBuilder fetchBinaryReport(ExecutionContext executionContext, URI pdfReportUrl) throws Exception {

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(pdfReportUrl)
            .build();

        logInfo(executionContext, "Fetching binary report from %s", pdfReportUrl);

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        logInfo(executionContext, "Response from %s returned status %s", pdfReportUrl, response.statusCode());

        if (response.statusCode() / 100 == 2) {
            return BinaryReport.builder()
                .bytes(response.body())
                .contentType("application/pdf");
        } else {
            throw new RuntimeException("Got status: " + response.statusCode());
        }
    }

    private static LocalDateTime parseDate(String date) {
        var zone = ZoneId.of("Europe/Warsaw");
        ZonedDateTime zonedDateTime = DateTimeUtils.parseDate(date, zone);
        return DateTimeUtils.toUtc(zonedDateTime);
    }

    @ToString
    static class PageWrapper {

        private final HtmlPage page;

        public PageWrapper(HtmlPage page) {
            this.page = page;
        }

        public int getAvalancheLevel() {
            List<HtmlElement> levels = page.getByXPath("//*[contains(concat(' ', normalize-space(@class), ' '), ' topr-mobile-region-level ')]");
            if (levels.isEmpty()) {
                throw new IllegalArgumentException("No avalanche levels found");
            }
            return levels.stream().mapToInt(element -> {
                String level = element.getTextContent().trim();
                if (!level.matches("[1-5]\\s*[-–].+")) {
                    throw new IllegalArgumentException("Unknown level: " + level);
                }
                return level.charAt(0) - '0';
            }).max().orElseThrow();
        }

        public String getReportDate() {
            return getDate("Data publikacji");
        }

        public String getReportExpirationDate() {
            return getDate("Obowiązuje do");
        }

        public String getComment() {
            List<HtmlElement> articles = page.getByXPath("//article[contains(concat(' ', normalize-space(@class), ' '), ' topr-bulletin ')]");
            if (articles.isEmpty()) {
                throw new IllegalArgumentException("No avalanche bulletins found");
            }
            return articles.stream().map(article -> {
                HtmlElement comment = article.querySelector(".avalanche-comment");
                if (comment == null) {
                    throw new IllegalArgumentException("No avalanche comment found");
                }
                List<HtmlElement> paragraphs = comment.getElementsByTagName("p");
                String text = paragraphs.stream().map(HtmlElement::getTextContent)
                    .map(String::trim).collect(Collectors.joining("\n"));
                if (text.isBlank()) {
                    throw new IllegalArgumentException("Empty avalanche comment");
                }
                if (articles.size() == 1) {
                    return text;
                }
                HtmlElement region = article.querySelector(".topr-mobile-detail-meta > div > strong");
                if (region == null) {
                    throw new IllegalArgumentException("No bulletin region found");
                }
                return region.getTextContent().trim() + ": " + text;
            }).collect(Collectors.joining("\n\n"));
        }

        public URI getPdfReportUrl() throws Exception {
            List<HtmlElement> links = page.getByXPath("//a[normalize-space(.)='Pobierz komunikat PDF']");
            if (links.size() != 1 || links.get(0).getAttribute("href").isBlank()) {
                throw new IllegalArgumentException("Expected a single PDF link");
            }
            return page.getFullyQualifiedUrl(links.get(0).getAttribute("href")).toURI();
        }

        private String getDate(String label) {
            List<HtmlElement> dates = page.getByXPath(
                "//*[contains(concat(' ', normalize-space(@class), ' '), ' topr-mobile-dates ')]/div[span[normalize-space(.)='" + label + "']]/strong");
            if (dates.size() != 1) {
                throw new IllegalArgumentException("Expected a single date for " + label);
            }
            return dates.get(0).getTextContent().trim();
        }
    }
}
