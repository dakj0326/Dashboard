package org.net.news;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class NewsService {
    private static final Duration MAX_AGE = Duration.ofHours(24);
    private static final int MAX_ARTICLES = 20;
    private static final List<Feed> FEEDS = List.of(
            new Feed("SVT Nyheter", URI.create("https://www.svt.se/nyheter/rss.xml")),
            new Feed("Ekot", URI.create("https://api.sr.se/api/rss/program/83"))
    );
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public FetchResult fetchLatest() {
        List<NewsArticle> articles = new ArrayList<>();
        int successfulSources = 0;
        for (Feed feed : FEEDS) {
            try {
                articles.addAll(fetch(feed));
                successfulSources++;
            } catch (Exception ignored) {
                // One source may be unavailable without taking down the combined feed.
            }
        }

        Instant cutoff = Instant.now().minus(MAX_AGE);
        Set<String> seenTitles = new HashSet<>();
        List<NewsArticle> current = articles.stream()
                .filter(article -> !article.publishedAt().isBefore(cutoff))
                .sorted(Comparator.comparing(NewsArticle::publishedAt).reversed())
                .filter(article -> seenTitles.add(normalizeTitle(article.title())))
                .limit(MAX_ARTICLES)
                .toList();
        return new FetchResult(current, successfulSources);
    }

    private List<NewsArticle> fetch(Feed feed) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(feed.uri())
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .header("User-Agent", "Kjellberius-Dashboard/1.0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("News feed returned " + response.statusCode());
        }
        return parse(response.body(), feed.name());
    }

    private static List<NewsArticle> parse(String xml, String source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        var document = factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(xml))
        );
        List<NewsArticle> articles = new ArrayList<>();
        addEntries(articles, document.getElementsByTagName("item"), source, false);
        addEntries(articles, document.getElementsByTagName("entry"), source, true);
        return articles;
    }

    private static void addEntries(
            List<NewsArticle> articles,
            NodeList items,
            String source,
            boolean atom
    ) {
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String title = text(item, "title");
            String link = atom ? atomLink(item) : text(item, "link");
            Instant published = parseDate(firstText(item, "pubDate", "published", "updated"));
            if (title.isBlank() || link.isBlank() || published == null) continue;
            articles.add(new NewsArticle(
                    title.strip(),
                    cleanSummary(firstText(item, "description", "summary")),
                    source,
                    published,
                    URI.create(link.strip())
            ));
        }
    }

    private static String atomLink(Element item) {
        NodeList links = item.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String relation = link.getAttribute("rel");
            if ((relation.isBlank() || "alternate".equals(relation))
                    && !link.getAttribute("href").isBlank()) {
                return link.getAttribute("href");
            }
        }
        return "";
    }

    private static String firstText(Element element, String... tags) {
        for (String tag : tags) {
            String value = text(element, tag);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static Instant parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.RFC_1123_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME
        )) {
            try {
                return ZonedDateTime.parse(value.strip(), formatter).toInstant();
            } catch (DateTimeParseException ignored) {
                try {
                    return OffsetDateTime.parse(value.strip(), formatter).toInstant();
                } catch (DateTimeParseException alsoIgnored) {
                    // Try the next common feed format.
                }
            }
        }
        return null;
    }

    private static String cleanSummary(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static String normalizeTitle(String title) {
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }

    private record Feed(String name, URI uri) {}

    public record FetchResult(List<NewsArticle> articles, int successfulSources) {}
}
