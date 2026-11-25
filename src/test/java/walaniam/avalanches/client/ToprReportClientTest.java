package walaniam.avalanches.client;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToprReportClientTest {

    @Test
    void parsesCurrentLayoutAndResolvesPdfLink() throws Exception {
        try (WebClient client = new WebClient()) {
            var page = page(client, """
                <div class="topr-mobile-dates">
                  <div><span>Data publikacji</span><strong>2026-11-25 17:14</strong></div>
                  <div><span>Obowiązuje do</span><strong>2026-11-26 20:00</strong></div>
                </div>
                <span class="extra topr-mobile-region-level">2 – umiarkowany</span>
                <article class="topr-bulletin">
                  <div class="avalanche-comment"><h4>Komentarz TOPR do komunikatu:</h4><p>First paragraph.</p><p>Second paragraph.</p></div>
                </article>
                <a href="/wp-content/uploads/caaml/pdf_pl_2026-11-25.pdf">Pobierz komunikat PDF</a>
                """);

            assertThat(page.getAvalancheLevel()).isEqualTo(2);
            assertThat(page.getReportDate()).isEqualTo("2026-11-25 17:14");
            assertThat(page.getReportExpirationDate()).isEqualTo("2026-11-26 20:00");
            assertThat(page.getComment()).isEqualTo("First paragraph.\nSecond paragraph.");
            assertThat(page.getPdfReportUrl()).isEqualTo(URI.create("https://lawiny.topr.pl/wp-content/uploads/caaml/pdf_pl_2026-11-25.pdf"));
        }
    }

    @Test
    void preservesRegionalCommentsAndUsesHighestLevel() throws Exception {
        try (WebClient client = new WebClient()) {
            var page = page(client, """
                <span class="topr-mobile-region-level">1 – niski</span>
                <span class="topr-mobile-region-level">3 – znaczny</span>
                <article class="topr-bulletin">
                  <div class="topr-mobile-detail-meta"><div><span>Region:</span><strong>West</strong></div></div>
                  <div class="avalanche-comment"><p>West conditions.</p></div>
                </article>
                <article class="topr-bulletin">
                  <div class="topr-mobile-detail-meta"><div><span>Region:</span><strong>East</strong></div></div>
                  <div class="avalanche-comment"><p>East conditions.</p></div>
                </article>
                """);

            assertThat(page.getAvalancheLevel()).isEqualTo(3);
            assertThat(page.getComment()).isEqualTo("West: West conditions.\n\nEast: East conditions.");
        }
    }

    @Test
    void rejectsUnknownLevelEvenWhenAnotherRegionIsValid() throws Exception {
        try (WebClient client = new WebClient()) {
            var page = page(client, """
                <span class="topr-mobile-region-level">2 – umiarkowany</span>
                <span class="topr-mobile-region-level">6 – unknown</span>
                """);

            assertThatThrownBy(page::getAvalancheLevel).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private ToprReportClient.PageWrapper page(WebClient client, String html) throws Exception {
        var url = URI.create("https://lawiny.topr.pl/").toURL();
        var connection = new MockWebConnection();
        connection.setResponse(url, "<html><head><meta charset=\"UTF-8\"></head><body>" + html + "</body></html>",
            "text/html", StandardCharsets.UTF_8);
        client.setWebConnection(connection);
        client.getOptions().setJavaScriptEnabled(false);
        return new ToprReportClient.PageWrapper(client.getPage(url));
    }
}
