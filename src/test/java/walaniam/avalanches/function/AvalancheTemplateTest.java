package walaniam.avalanches.function;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvalancheTemplateTest {

    @Test
    void showsGapsIncludingPageBoundaryWithoutCreatingFakeReports() throws Exception {
        try (WebClient client = new WebClient()) {
            URL url = new URL("http://localhost/avalanche");
            String html;
            try (var stream = getClass().getResourceAsStream("/avalanche_template.html")) {
                html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            String chartStub = """
                <script>
                let chart;
                HTMLCanvasElement.prototype.getContext = function() { return {}; };
                function Chart(ctx, config) {
                  chart = config;
                  this.destroy = function() {};
                }
                </script>
                """;
            html = html.replace("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>", chartStub);
            // HtmlUnit's JavaScript engine does not support async functions.
            int loadStart = html.indexOf("    async function loadReports(");
            int chartStart = html.indexOf("    function createChart(", loadStart);
            html = html.substring(0, loadStart) + "    function loadReports() {}\n\n" + html.substring(chartStart);
            MockWebConnection connection = new MockWebConnection();
            connection.setResponse(url, html);
            client.setWebConnection(connection);

            HtmlPage page = client.getPage(url);
            page.executeJavaScript("""
                createChart(withMissingDays([
                  {reportExpirationDate: '2026-11-24T06:00:00', avalancheLevel: 1, comment: 'second'},
                  {reportExpirationDate: '2026-11-24T12:00:00', avalancheLevel: 2, comment: 'first'},
                  {reportExpirationDate: '2026-11-26T12:00:00', avalancheLevel: 3}
                ], {reportExpirationDate: '2026-11-22T12:00:00'}))
                """);

            assertEquals("Missing reports: 2026-11-23, 2026-11-25",
                page.getElementById("missingReports").getTextContent());
            assertEquals("[null,1,2,null,3]",
                page.executeJavaScript("JSON.stringify(chart.data.datasets[0].data)").getJavaScriptResult());
            assertEquals(2, ((Number) page.executeJavaScript("""
                var rectangles = 0;
                chart.plugins[0].afterDatasetsDraw({
                  ctx: {save() {}, restore() {}, fillRect() { rectangles++; }},
                  chartArea: {top: 0, bottom: 100},
                  scales: {x: {width: 500, getPixelForValue: index => index * 100}}
                });
                rectangles;
                """).getJavaScriptResult()).intValue());
            page.executeJavaScript("chart.options.onClick(null, [{index: 0}])");
            assertEquals("none", page.executeJavaScript("document.getElementById('commentBox').style.display")
                .getJavaScriptResult());
            page.executeJavaScript("chart.options.onClick(null, [{index: 2}])");
            assertEquals("first", page.getElementById("commentText").getTextContent());

            page.executeJavaScript("""
                createChart(withMissingDays([
                  {reportExpirationDate: '2026-11-22T12:00:00', avalancheLevel: 2}
                ], null))
                """);
            assertTrue(page.getElementById("missingReports").hasAttribute("hidden"));
            assertEquals("[\"2026-11-22\"]",
                page.executeJavaScript("JSON.stringify(chart.data.labels)").getJavaScriptResult());
            assertEquals("[\"2024-02-28\",\"2024-02-29\",\"2024-03-01\"]",
                page.executeJavaScript("""
                    JSON.stringify(withMissingDays([
                      {reportExpirationDate: '2024-02-28T12:00:00Z'},
                      {reportExpirationDate: '2024-03-01T12:00:00Z'}
                    ], null).map(entry => entry.date))
                    """).getJavaScriptResult());
            assertEquals("[]", page.executeJavaScript("JSON.stringify(withMissingDays([], null))")
                .getJavaScriptResult());
        }
    }
}
