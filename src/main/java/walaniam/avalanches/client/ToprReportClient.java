package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import lombok.ToString;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
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
import java.util.Optional;
import java.util.Set;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

public class ToprReportClient implements AvalancheReportClient {

    private static final String REPORTER = "topr";
    private final String reportUrl = "https://lawiny.topr.pl/";
    private final String pdfReportUrl = "https://lawiny.topr.pl/viewpdf";

    @Override
    public ReportFetchResult fetch(ExecutionContext executionContext) throws ReportFetchException {
        int errors = 0;
        Exception lastException;
        do {
            logInfo(executionContext, "Fetching report: %s", reportUrl);

            try (WebClient webClient = new WebClient()) {
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setJavaScriptEnabled(true);

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
                    BinaryReport binaryReport = fetchBinaryReport(executionContext)
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

    private BinaryReport.BinaryReportBuilder fetchBinaryReport(ExecutionContext executionContext) throws Exception {

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(pdfReportUrl))
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
    private static class PageWrapper {

        private static final Set<String> KNOWN_LEVELS = Set.of("1", "2", "3", "4", "5");

        private final HtmlPage page;
        private final HtmlElement pageElement;

        public PageWrapper(HtmlPage page) {
            this.page = page;
            this.pageElement = page.getDocumentElement();
        }

        public int getAvalancheLevel() {
            String level = findSingleElementsByClassName(pageElement, "span", "law-mst-lev")
                .getTextContent()
                .trim();
            if (!KNOWN_LEVELS.contains(level)) {
                throw new IllegalArgumentException("Unknown level: " + level);
            }
            return Integer.parseInt(level);
        }

        public String getReportDate() {
            return findSingleElementsByClassName(pageElement, "div", "law-mst-iat")
                .getTextContent()
                .trim();
        }

        public String getReportExpirationDate() {
            return findSingleElementsByClassName(pageElement, "div", "law-mst-exp")
                .getTextContent()
                .trim();
        }

        public String getComment() {
            return Optional.ofNullable(page.getElementById("law-comment"))
                .map(DomElement::getTextContent)
                .map(String::trim)
                .orElseThrow();
        }

        private static HtmlElement findSingleElementsByClassName(HtmlElement element, String tagName, String className) {
            List<HtmlElement> elements = element.getElementsByTagName(tagName)
                .stream()
                .filter(it -> it.hasAttribute("class") && className.equals(it.getAttribute("class")))
                .toList();
            if (elements.size() != 1) {
                throw new IllegalArgumentException("Expected single element: " + tagName + " with class " + className);
            }
            return elements.get(0);
        }
    }
}
