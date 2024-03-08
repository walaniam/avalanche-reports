package walaniam.avalanches.client;

import com.microsoft.azure.functions.ExecutionContext;
import lombok.ToString;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import walaniam.avalanches.persistence.AvalancheReport;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static walaniam.avalanches.common.logging.LoggingUtils.logInfo;
import static walaniam.avalanches.common.logging.LoggingUtils.logWarn;

public class ToprReportClient implements AvalancheReportClient {

    private final String reportUrl = "https://lawiny.topr.pl/";

    @Override
    public AvalancheReport fetch(ExecutionContext executionContext) throws ReportFetchException {
        int errors = 0;
        Exception lastException;
        do {
            logInfo(executionContext, "Fetching report: %s", reportUrl);

            try (WebClient webClient = new WebClient()) {
                webClient.getOptions().setCssEnabled(false);
                webClient.getOptions().setJavaScriptEnabled(true);

                HtmlPage page = webClient.getPage(reportUrl);

                var pageWrapper = new PageWrapper(page);

                return AvalancheReport.builder()
                    .avalancheLevel(pageWrapper.getAvalancheLevel())
                    .reportDate(parseDate(pageWrapper.getReportDate()))
                    .reportExpirationDate(parseDate(pageWrapper.getReportExpirationDate()))
                    .comment(pageWrapper.getComment())
                    .reportedBy("topr")
                    .build();

            } catch (IOException e) {
                logWarn(executionContext, "Failed", e);
                errors++;
                lastException = e;
            }
        } while (errors < 2);

        throw new ReportFetchException("Fetch failed after attempts: " + errors, lastException);
    }

    private static LocalDateTime parseDate(String date) {
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(date, formatter);
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
