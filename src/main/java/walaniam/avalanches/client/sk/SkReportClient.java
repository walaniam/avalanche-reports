package walaniam.avalanches.client.sk;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fetches and parses the Slovak avalanche bulletin from laviny.sk in CAAMLv5 BulletinEAWS format.
 */
public class SkReportClient {

    static final String REPORT_URL = "https://static.laviny.sk/bulletins/latest/en.xml";

    private static final String CAAML_NS = "http://caaml.org/Schemas/V5.0/Profiles/BulletinEAWS";
    private static final String GML_NS = "http://www.opengis.net/gml";

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_DATE_TIME;

    private final String reportUrl;

    public SkReportClient() {
        this(REPORT_URL);
    }

    SkReportClient(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    /**
     * Fetches the XML report from the configured URL and parses it.
     */
    public List<SkBulletin> fetch() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(new URI(reportUrl))
            .timeout(Duration.ofSeconds(30))
            .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Unexpected HTTP status " + response.statusCode() + " fetching " + reportUrl);
        }
        return parse(new ByteArrayInputStream(response.body()));
    }

    /**
     * Parses a CAAMLv5 BulletinEAWS XML stream into a {@link List}.
     */
    public List<SkBulletin> parse(InputStream xmlStream) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlStream);

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new CaamlNamespaceContext());

        NodeList bulletinNodes = (NodeList) xpath.evaluate(
            "/caaml:ObsCollection/caaml:observations/caaml:Bulletin",
            doc, XPathConstants.NODESET);

        List<SkBulletin> bulletins = new ArrayList<>();
        for (int i = 0; i < bulletinNodes.getLength(); i++) {
            Node bulletinNode = bulletinNodes.item(i);
            bulletins.add(parseBulletin(xpath, bulletinNode));
        }

        return bulletins;
    }

    private SkBulletin parseBulletin(XPath xpath, Node bulletinNode) throws Exception {
        String beginPosition = xpath.evaluate("caaml:validTime/caaml:TimePeriod/caaml:beginPosition", bulletinNode);
        String endPosition = xpath.evaluate("caaml:validTime/caaml:TimePeriod/caaml:endPosition", bulletinNode);

        NodeList locRefNodes = (NodeList) xpath.evaluate(
            "caaml:locRef",
            bulletinNode, XPathConstants.NODESET);

        Set<String> regionIds = new LinkedHashSet<>();
        for (int i = 0; i < locRefNodes.getLength(); i++) {
            Node locRefNode = locRefNodes.item(i);
            String href = locRefNode.getAttributes().getNamedItemNS(
                "http://www.w3.org/1999/xlink", "href").getNodeValue();
            if (href != null && !href.isBlank()) {
                regionIds.add(href);
            }
        }

        NodeList dangerRatingNodes = (NodeList) xpath.evaluate(
            "caaml:bulletinResultsOf/caaml:BulletinMeasurements/caaml:dangerRatings/caaml:DangerRating",
            bulletinNode, XPathConstants.NODESET);

        List<Integer> dangerRatings = new ArrayList<>();
        for (int i = 0; i < dangerRatingNodes.getLength(); i++) {
            Node drNode = dangerRatingNodes.item(i);
            String mainValue = xpath.evaluate("caaml:mainValue", drNode).trim();
            if (!mainValue.isEmpty()) {
                dangerRatings.add(Integer.parseInt(mainValue));
            }
        }

        String highlights = xpath.evaluate(
            "caaml:bulletinResultsOf/caaml:BulletinMeasurements/caaml:avActivityHighlights",
            bulletinNode).trim();
        String comment = xpath.evaluate(
            "caaml:bulletinResultsOf/caaml:BulletinMeasurements/caaml:avActivityComment",
            bulletinNode).trim();
        String snowpackStructureComment = xpath.evaluate(
            "caaml:bulletinResultsOf/caaml:BulletinMeasurements/caaml:snowpackStructureComment",
            bulletinNode).trim();

        LocalDateTime beginDate = parseIsoDateTime(beginPosition);
        LocalDateTime endDate = parseIsoDateTime(endPosition);

        return new SkBulletin(regionIds, beginDate, endDate, dangerRatings, highlights, comment, snowpackStructureComment);
    }

    private static LocalDateTime parseIsoDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), ISO_DT);
    }

    /**
     * Simple {@link NamespaceContext} that maps {@code caaml} to the CAAMLv5 BulletinEAWS namespace
     * and {@code gml} to the OpenGIS GML namespace.
     */
    private static final class CaamlNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            return switch (prefix) {
                case "caaml" -> CAAML_NS;
                case "gml" -> GML_NS;
                default -> javax.xml.XMLConstants.NULL_NS_URI;
            };
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    }
}

