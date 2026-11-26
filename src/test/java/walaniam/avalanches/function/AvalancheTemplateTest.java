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
                  {reportExpirationDate: '2026-11-22T12:00:00', avalancheLevel: 2},
                  {reportExpirationDate: '2026-11-24T06:00:00', avalancheLevel: 1, comment: 'second'},
                  {reportExpirationDate: '2026-11-24T12:00:00', avalancheLevel: 2, comment: 'first'},
                  {reportExpirationDate: '2026-11-26T12:00:00', avalancheLevel: 3}
                ]))
                """);

            assertEquals("Missing reports: 2026-11-23, 2026-11-25",
                page.getElementById("missingReports").getTextContent());
            assertEquals("[2,null,1,2,null,3]",
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
            page.executeJavaScript("chart.options.onClick(null, [{index: 1}])");
            assertEquals("none", page.executeJavaScript("document.getElementById('commentBox').style.display")
                .getJavaScriptResult());
            page.executeJavaScript("chart.options.onClick(null, [{index: 3}])");
            assertEquals("first", page.getElementById("commentText").getTextContent());

            page.executeJavaScript("""
                createChart(withMissingDays([
                  {reportExpirationDate: '2026-11-22T12:00:00', avalancheLevel: 2}
                ]))
                """);
            assertTrue(page.getElementById("missingReports").hasAttribute("hidden"));
            assertEquals("[\"2026-11-22\"]",
                page.executeJavaScript("JSON.stringify(chart.data.labels)").getJavaScriptResult());
            assertEquals("[\"2024-02-28\",\"2024-02-29\",\"2024-03-01\"]",
                page.executeJavaScript("""
                    JSON.stringify(withMissingDays([
                      {reportExpirationDate: '2024-02-28T12:00:00Z'},
                      {reportExpirationDate: '2024-03-01T12:00:00Z'}
                    ]).map(entry => entry.date))
                    """).getJavaScriptResult());
            assertEquals("[]", page.executeJavaScript("JSON.stringify(withMissingDays([]))")
                .getJavaScriptResult());
            assertEquals("[20,20,1,0]",
                page.executeJavaScript("""
                    var reports = [];
                    for (var day = 1; day <= 40; day++) {
                      if (day === 20 || day === 21) continue;
                      var date = new Date(Date.UTC(2026, 0, day, 12)).toISOString();
                      reports.push({reportExpirationDate: date, avalancheLevel: 2});
                      if (day === 25) reports.push({reportExpirationDate: date, avalancheLevel: 3});
                    }
                    var entries = withMissingDays(reports);
                    JSON.stringify([0, 1, 2, 3].map(p => entriesForPage(entries, p).length));
                    """).getJavaScriptResult());
            assertEquals("[\"2026-01-20\",\"2026-01-21\"]",
                page.executeJavaScript("""
                    JSON.stringify(entriesForPage(entries, 1)
                      .filter(entry => !entry.report).map(entry => entry.date))
                    """).getJavaScriptResult());
            assertEquals(true, page.executeJavaScript("""
                JSON.stringify(entriesForPage(entries, 2).concat(entriesForPage(entries, 1),
                  entriesForPage(entries, 0))) === JSON.stringify(entries)
                """).getJavaScriptResult());
            page.executeJavaScript("""
                updateReportSummaries([
                  {reportDate: '2026-11-18T16:00:00', reportExpirationDate: '2026-11-19T19:00:00', avalancheLevel: 5},
                  {reportDate: '2026-11-21T16:00:00', reportExpirationDate: '2026-11-22T19:00:00', avalancheLevel: 1},
                  {reportDate: '2026-11-22T16:00:00', reportExpirationDate: '2026-11-23T19:00:00', avalancheLevel: 2},
                  {reportDate: '2026-11-23T16:00:00', reportExpirationDate: '2026-11-24T19:00:00', avalancheLevel: 2},
                  {reportDate: '2026-11-24T16:00:00', reportExpirationDate: '2026-11-25T19:00:00', avalancheLevel: 2},
                  {reportDate: '2026-11-25T16:00:00', reportExpirationDate: '2026-11-26T19:00:00', avalancheLevel: 3}
                ], '2026-11-26')
                """);
            assertEquals("Danger level changed from 2 to 3 (increased by 1).",
                page.getElementById("reportChange").getTextContent());
            assertEquals("Level 1: 1 dayLevel 2: 3 daysLevel 3: 1 dayLevel 4: 0 daysLevel 5: 0 daysNo report: 2 days",
                page.getElementById("weeklyLevelCounts").getTextContent());
        }
    }
}
