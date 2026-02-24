package walaniam.avalanches.client.sk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.persistence.ReportId;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkReportClientTest {

    private SkReportClient underTest;

    @BeforeEach
    void setUp() {
        underTest = new SkReportClient();
    }

    @Test
    void shouldParseXmlIntoCollection() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        assertThat(xml).as("Test resource report_en.xml must be present on classpath").isNotNull();

        List<SkBulletin> bulletins = underTest.parse(xml);

        assertThat(bulletins).hasSize(3);
    }

    @Test
    void shouldExtractCorrectMaxDangerRating() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        List<SkBulletin> bulletins = underTest.parse(xml);

        // First bulletin has ratings [3, 2] => max 3 across all bulletins
        int maxDangerRating = bulletins.stream()
            .mapToInt(SkBulletin::getMaxDangerRating)
            .max()
            .orElse(0);
        assertThat(maxDangerRating).isEqualTo(3);
    }

    @Test
    void shouldExtractValidTime() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        List<SkBulletin> bulletins = underTest.parse(xml);

        SkBulletin first = bulletins.get(0);
        LocalDateTime begin = first.getBeginPosition();
        LocalDateTime end = first.getEndPosition();

        assertThat(begin).isNotNull();
        assertThat(end).isNotNull();
        assertThat(end).isAfter(begin);

        // The first bulletin begins on 2026-02-23T23:00:00Z and ends 2026-02-24T23:00:00Z
        assertThat(begin).isEqualTo(LocalDateTime.of(2026, 2, 23, 23, 0, 0));
        assertThat(end).isEqualTo(LocalDateTime.of(2026, 2, 24, 23, 0, 0));
    }

    @Test
    void shouldExtractDangerRatingsPerBulletin() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        List<SkBulletin> bulletins = underTest.parse(xml);

        // Bulletin 1: [3, 2]
        assertThat(bulletins.get(0).getDangerRatings()).containsExactly(3, 2);
        assertThat(bulletins.get(0).getMaxDangerRating()).isEqualTo(3);
        // Bulletin 2: [2, 1]
        assertThat(bulletins.get(1).getDangerRatings()).containsExactly(2, 1);
        assertThat(bulletins.get(1).getMaxDangerRating()).isEqualTo(2);
        // Bulletin 3: [1, 1]
        assertThat(bulletins.get(2).getDangerRatings()).containsExactly(1, 1);
        assertThat(bulletins.get(2).getMaxDangerRating()).isEqualTo(1);
    }

    @Test
    void shouldExtractNonBlankComment() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        List<SkBulletin> bulletins = underTest.parse(xml);

        boolean anyNonBlankComment = bulletins.stream()
            .map(SkBulletin::getComment)
            .anyMatch(c -> c != null && !c.isBlank());
        assertThat(anyNonBlankComment).isTrue();
    }

    @Test
    void adapterShouldMapBulletinToReports() throws Exception {
        InputStream xml = getClass().getResourceAsStream("/walaniam/avalanches/client/sk/report_en.xml");
        List<SkBulletin> bulletins = new SkReportClient().parse(xml);

        SkReportAdapter adapter = new SkReportAdapter();
        // Use the first bulletin (highest danger rating = 3)
        SkBulletin firstBulletin = bulletins.get(0);
        List<AvalancheReport> reports = adapter.adapt(firstBulletin);

        assertThat(reports).isNotEmpty();

        AvalancheReport report = reports.get(0);
        assertThat(report).isNotNull();
        assertThat(report.getAvalancheLevel()).isEqualTo(3);
        assertThat(report.getReportDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isNotNull();
        assertThat(report.getReportExpirationDate()).isAfter(report.getReportDate());
        assertThat(report.getComment()).isNotBlank();
        assertThat(report.getId()).isNotNull();
        assertThat(report.getId())
            .extracting(ReportId::getReportedBy)
            .isEqualTo(SkReportAdapter.REPORTER);
        assertThat(report.getId().getReportDate()).isEqualTo(report.getReportDate());
    }
}
