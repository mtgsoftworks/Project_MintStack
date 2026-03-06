package com.mintstack.finance.service.external;

import com.mintstack.finance.config.NewsFeedProperties;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssNewsClient {

    private static final DateTimeFormatter ALT_RSS_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final NewsCategoryRepository categoryRepository;
    private final NewsRepository newsRepository;
    private final NewsFeedProperties newsFeedProperties;

    @CircuitBreaker(name = "rssNewsApi", fallbackMethod = "fetchAllNewsFallback")
    @Retry(name = "externalApi")
    public List<News> fetchAllNews() {
        List<NewsFeedProperties.Feed> feeds = newsFeedProperties.getFeeds().stream()
            .filter(NewsFeedProperties.Feed::isEnabled)
            .filter(feed -> StringUtils.hasText(feed.getUrl()))
            .sorted(Comparator.comparingInt(NewsFeedProperties.Feed::getPriority).thenComparing(feed -> defaultString(feed.getCode())))
            .toList();

        if (feeds.isEmpty()) {
            log.warn("No active RSS feed configured. Returning cached DB news.");
            return newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(PageRequest.of(0, 50)).getContent();
        }

        List<News> allNews = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();

        for (NewsFeedProperties.Feed feed : feeds) {
            try {
                List<News> feedNews = fetchNewsFromFeed(feed);
                int added = 0;
                for (News news : feedNews) {
                    String dedupKey = buildDedupKey(news);
                    if (seenKeys.add(dedupKey)) {
                        allNews.add(news);
                        added++;
                    }
                }
                log.info("Fetched {} news ({} unique) from feed {}", feedNews.size(), added, feed.getCode());
            } catch (Exception e) {
                log.warn("Failed to fetch feed {} ({}): {}", feed.getCode(), feed.getUrl(), e.getMessage());
            }
        }

        return allNews;
    }

    public List<News> fetchAllNewsFallback(Exception e) {
        log.warn("RSS News fallback triggered: {}", e.getMessage());
        return newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(PageRequest.of(0, 50)).getContent();
    }

    private List<News> fetchNewsFromFeed(NewsFeedProperties.Feed feed) {
        List<News> newsList = new ArrayList<>();
        NewsCategory category = resolveCategory(feed.getCategorySlug());
        if (category == null) {
            log.warn("No category found for feed {} (slug={})", feed.getCode(), feed.getCategorySlug());
            return newsList;
        }

        Document doc = fetchRssDocument(feed.getUrl());
        NodeList items = resolveItems(doc);
        int maxItems = Math.max(1, newsFeedProperties.getMaxItemsPerFeed());

        for (int i = 0; i < Math.min(items.getLength(), maxItems); i++) {
            Node node = items.item(i);
            if (!(node instanceof Element item)) {
                continue;
            }

            String title = firstNonBlank(
                getElementText(item, "title"),
                getElementText(item, "media:title")
            );
            if (!StringUtils.hasText(title)) {
                continue;
            }

            String summary = cleanHtml(firstNonBlank(
                getElementText(item, "description"),
                getElementText(item, "content:encoded"),
                getElementText(item, "summary"),
                getElementText(item, "content")
            ));
            String content = summary;
            String sourceUrl = extractLink(item);
            LocalDateTime publishedAt = parseDate(firstNonBlank(
                getElementText(item, "pubDate"),
                getElementText(item, "published"),
                getElementText(item, "updated"),
                getElementText(item, "dc:date")
            ));
            String sourceName = resolveSourceName(feed);

            News news = News.builder()
                .title(title.trim())
                .summary(summary)
                .content(content)
                .sourceUrl(sourceUrl)
                .sourceName(sourceName)
                .imageUrl(extractImageUrl(item))
                .category(category)
                .publishedAt(publishedAt)
                .isPublished(true)
                .isSimulated(false)
                .externalHash(buildExternalHash(title, sourceName, sourceUrl, publishedAt))
                .viewCount(0L)
                .build();

            newsList.add(news);
        }

        return newsList;
    }

    private NewsCategory resolveCategory(String categorySlug) {
        if (StringUtils.hasText(categorySlug)) {
            Optional<NewsCategory> bySlug = categoryRepository.findBySlug(categorySlug.trim());
            if (bySlug.isPresent()) {
                return bySlug.get();
            }
        }

        List<NewsCategory> active = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        if (!active.isEmpty()) {
            return active.get(0);
        }
        return categoryRepository.findAll().stream().findFirst().orElse(null);
    }

    private NodeList resolveItems(Document doc) {
        NodeList rssItems = doc.getElementsByTagName("item");
        if (rssItems.getLength() > 0) {
            return rssItems;
        }
        return doc.getElementsByTagName("entry");
    }

    private Document fetchRssDocument(String rssUrl) {
        try {
            URL url = new URL(rssUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "MintStack Finance Bot/2.0");
            connection.setConnectTimeout(newsFeedProperties.getConnectTimeoutMs());
            connection.setReadTimeout(newsFeedProperties.getReadTimeoutMs());

            try (InputStream inputStream = connection.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(inputStream);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error fetching RSS document from " + rssUrl, e);
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return defaultString(nodes.item(0).getTextContent()).trim();
        }
        return null;
    }

    private String cleanHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        return html
            .replaceAll("<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String extractLink(Element item) {
        String classic = firstNonBlank(getElementText(item, "link"), getElementText(item, "guid"));
        if (StringUtils.hasText(classic)) {
            return classic.trim();
        }

        NodeList links = item.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Node node = links.item(i);
            if (node instanceof Element linkEl) {
                String href = linkEl.getAttribute("href");
                if (StringUtils.hasText(href)) {
                    return href.trim();
                }
            }
        }
        return null;
    }

    private String extractImageUrl(Element item) {
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enclosures.getLength(); i++) {
            Node node = enclosures.item(i);
            if (node instanceof Element enclosure) {
                String type = enclosure.getAttribute("type");
                String url = enclosure.getAttribute("url");
                if (StringUtils.hasText(url) && (!StringUtils.hasText(type) || type.startsWith("image"))) {
                    return url;
                }
            }
        }

        String mediaContent = getAttributeFromTag(item, "media:content", "url");
        if (StringUtils.hasText(mediaContent)) {
            return mediaContent;
        }

        String mediaThumbnail = getAttributeFromTag(item, "media:thumbnail", "url");
        if (StringUtils.hasText(mediaThumbnail)) {
            return mediaThumbnail;
        }

        return null;
    }

    private String getAttributeFromTag(Element item, String tagName, String attribute) {
        NodeList nodes = item.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) instanceof Element element) {
            String value = element.getAttribute(attribute);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now();
        }

        String input = value.trim();
        try {
            return ZonedDateTime.parse(input, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(input, ALT_RSS_DATE).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            return ZonedDateTime.parse(input, DateTimeFormatter.ISO_ZONED_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
    }

    private String resolveSourceName(NewsFeedProperties.Feed feed) {
        if (StringUtils.hasText(feed.getSourceName())) {
            return feed.getSourceName().trim();
        }

        try {
            URI uri = URI.create(feed.getUrl());
            String host = defaultString(uri.getHost()).replace("www.", "");
            if (StringUtils.hasText(host)) {
                return host;
            }
        } catch (Exception ignored) {
        }

        return "Finans Haberleri";
    }

    private String buildExternalHash(String title, String sourceName, String sourceUrl, LocalDateTime publishedAt) {
        String raw = String.join("|",
            defaultString(title).trim().toLowerCase(Locale.ROOT),
            defaultString(sourceName).trim().toLowerCase(Locale.ROOT),
            defaultString(sourceUrl).trim().toLowerCase(Locale.ROOT),
            publishedAt != null ? publishedAt.toString() : ""
        );
        return sha256(raw);
    }

    private String buildDedupKey(News news) {
        if (StringUtils.hasText(news.getSourceUrl())) {
            return "url:" + news.getSourceUrl().trim().toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(news.getExternalHash())) {
            return "hash:" + news.getExternalHash();
        }
        return "title:" + defaultString(news.getTitle()).trim().toLowerCase(Locale.ROOT) + "|" + defaultString(news.getSourceName());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                builder.append(String.format("%02x", aByte));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}

