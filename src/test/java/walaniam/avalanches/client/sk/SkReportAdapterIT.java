package walaniam.avalanches.client.sk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.client.api.ReportFetchException;
import walaniam.avalanches.client.api.ReportFetchResult;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@WireMockTest
class SkReportAdapterIT {

    private final ExecutionContext executionContext = mock(ExecutionContext.class);

    private SkReportAdapter underTest;

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wireMockInfo) throws IOException {
        doReturn(Logger.getGlobal()).when(executionContext).getLogger();

        String baseUrl = wireMockInfo.getHttpBaseUrl();

        // Stub XML report endpoint
        byte[] xmlBytes;
        try (InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml")) {
            assertThat(xml).as("Test resource report_en.xml must be present on classpath").isNotNull();
            xmlBytes = xml.readAllBytes();
        }
        stubFor(get(urlEqualTo("/bulletins/latest/en.xml"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/xml")
                .withBody(xmlBytes)));

        // Stub binary PDF report endpoint (matches any date pattern)
        stubFor(get(urlMatching("/bulletins/\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}_SK_en\\.pdf"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withBody(loadPdfReport()))); // dummy PDF magic bytes

        SkReportClient skReportClient = new SkReportClient(baseUrl + "/bulletins/latest/en.xml");
        String binaryUrlTemplate = baseUrl + "/bulletins/{DATE}/{DATE}_SK_en.pdf";
        underTest = new SkReportAdapter(skReportClient, binaryUrlTemplate);
    }

    private byte[] loadPdfReport() throws IOException {
        try (InputStream pdf = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/binary.pdf")) {
            return pdf.readAllBytes();
        }
    }

    @Test
    void shouldFetchCurrentReport() throws ReportFetchException {
        ReportFetchResult fetchResult = underTest.fetch(executionContext);

        AvalancheReport report = fetchResult.getReport();
        assertThat(report).isNotNull();
        assertThat(report.getAvalancheLevel()).isIn(1, 2, 3, 4, 5);
        assertThat(report.getReportDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isAfter(report.getReportDate());
        assertThat(report.getComment()).isNotBlank();
        assertThat(report.getId()).isNotNull();
        assertThat(report.getId())
            .extracting(ReportId::getReportedBy)
            .isEqualTo("sk-laviny");
        assertThat(report.getId().getReportDate()).isEqualTo(report.getReportDate());

        assertThat(fetchResult.getBinaryReport()).isNotNull();
        assertThat(fetchResult.getBinaryReport().getBytes()).isNotEmpty();
    }
}
