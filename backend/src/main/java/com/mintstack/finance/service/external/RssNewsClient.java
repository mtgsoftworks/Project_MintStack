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
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssNewsClient {

    private static final DateTimeFormatter ALT_RSS_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final String COMPANY_CATEGORY_SLUG = "sirket";
    private static final String PREVIEW_USER_AGENT = "MintStack News Extractor/2.0";
    private static final Pattern META_TAG_PATTERN = Pattern.compile("<meta\\s+[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_BLOCK_PATTERN = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE_BLOCK_PATTERN = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("(?is)<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Set<String> COMPANY_POSITIVE_KEYWORDS = Set.of(
        "earnings", "revenue", "guidance", "quarter", "q1 ", "q2 ", "q3 ", "q4 ",
        "merger", "acquisition", "buyback", "dividend", "shares", "stock", "hisse",
        "hissesi", "hisseleri", "bilanco", "halka arz", "ceo", "holding", "inc", "corp"
    );
    private static final Set<String> COMPANY_NEGATIVE_KEYWORDS = Set.of(
        "enflasyon", "faiz", "merkez bankasi", "tcmb", "ppk", "makro", "issizlik",
        "cari acik", "gsyh", "parasal", "istatistik", "veri", "duyuru", "rapor",
        "hazine", "bakanlik", "regulasyon", "policy"
    );
    private static final Set<String> WEAK_SUMMARY_PREFIXES = Set.of(
        "source:", "kaynak:", "read more", "devami", "click here"
    );
    private static final Set<String> WEAK_SUMMARY_SUFFIXES = Set.of(
        "more", "read more", "devami", "details"
    );

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
        Map<String, ArticlePreview> previewCache = new HashMap<>();
        NewsCategory defaultCategory = resolveCategory(feed.getCategorySlug());
        if (defaultCategory == null) {
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

            String rawSummary = firstNonBlank(
                getElementText(item, "description"),
                getElementText(item, "content:encoded"),
                getElementText(item, "summary"),
                getElementText(item, "content")
            );

            String sourceUrl = extractLink(item);
            String sourceName = resolveSourceName(feed);
            String imageUrl = firstNonBlank(
                extractImageUrl(item),
                resolveAbsoluteUrl(sourceUrl, extractImageUrlFromMarkup(rawSummary))
            );

            String summary = cleanHtml(rawSummary);
            if (shouldFetchArticlePreview(sourceUrl, summary, title, imageUrl)) {
                ArticlePreview preview = previewCache.computeIfAbsent(
                    sourceUrl.toLowerCase(Locale.ROOT),
                    key -> fetchArticlePreviewSafe(sourceUrl)
                );
                summary = chooseBestSummary(summary, preview.summary(), title);
                imageUrl = firstNonBlank(imageUrl, preview.imageUrl());
            }

            summary = normalizeSummary(summary, title);

            NewsCategory category = resolveCategoryForItem(defaultCategory, title, summary, sourceName, sourceUrl);
            String content = summary;
            LocalDateTime publishedAt = parseDate(firstNonBlank(
                getElementText(item, "pubDate"),
                getElementText(item, "published"),
                getElementText(item, "updated"),
                getElementText(item, "dc:date")
            ));

            News news = News.builder()
                .title(title.trim())
                .summary(summary)
                .content(content)
                .sourceUrl(sourceUrl)
                .sourceName(sourceName)
                .imageUrl(imageUrl)
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

    private NewsCategory resolveCategoryForItem(NewsCategory defaultCategory, String title, String summary, String sourceName, String sourceUrl) {
        if (defaultCategory == null) {
            return null;
        }
        if (COMPANY_CATEGORY_SLUG.equalsIgnoreCase(defaultCategory.getSlug())) {
            return defaultCategory;
        }

        if (!looksLikeCompanyNews(title, summary, sourceName, sourceUrl)) {
            return defaultCategory;
        }

        return categoryRepository.findBySlug(COMPANY_CATEGORY_SLUG).orElse(defaultCategory);
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
        String unescaped = HtmlUtils.htmlUnescape(html);
        return unescaped
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .replaceAll("<[^>]*>", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#039;", "'")
            .replace("&#8217;", "'")
            .replace("&#x27;", "'")
            .replace("&#8211;", "-")
            .replace("&#8212;", "-")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean looksLikeCompanyNews(String title, String summary, String sourceName, String sourceUrl) {
        String source = (defaultString(sourceName) + " " + defaultString(sourceUrl)).toLowerCase(Locale.ROOT);
        if (source.contains("tcmb.gov.tr")) {
            return false;
        }

        String text = (defaultString(title) + " " + defaultString(summary)).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return false;
        }

        for (String negative : COMPANY_NEGATIVE_KEYWORDS) {
            if (text.contains(negative)) {
                return false;
            }
        }

        int score = 0;
        for (String positive : COMPANY_POSITIVE_KEYWORDS) {
            if (text.contains(positive)) {
                score++;
            }
        }

        if (text.contains(" kar acikladi") || text.contains(" zarar acikladi")) {
            score += 2;
        }
        if (text.contains(" a.s.") || text.contains(" anonim sirket")) {
            score++;
        }

        boolean strongSignal = text.contains("shares")
            || text.contains("earnings")
            || text.contains("dividend")
            || text.contains("acquisition")
            || text.contains("merger")
            || text.contains("hisse")
            || text.contains("ceo");

        return score >= 2 || (score >= 1 && strongSignal);
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

        String mediaContentAnyNamespace = getImageUrlFromAnyNode(item, "content");
        if (StringUtils.hasText(mediaContentAnyNamespace)) {
            return mediaContentAnyNamespace;
        }

        String mediaThumbnailAnyNamespace = getImageUrlFromAnyNode(item, "thumbnail");
        if (StringUtils.hasText(mediaThumbnailAnyNamespace)) {
            return mediaThumbnailAnyNamespace;
        }

        return extractImageFromChildNodes(item);
    }

    private String extractImageUrlFromMarkup(String markup) {
        if (!StringUtils.hasText(markup)) {
            return null;
        }
        Matcher matcher = IMAGE_SRC_PATTERN.matcher(markup);
        if (!matcher.find()) {
            return null;
        }
        String url = matcher.group(1);
        return StringUtils.hasText(url) ? url.trim() : null;
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

    private String getImageUrlFromAnyNode(Element item, String fragment) {
        String lowered = defaultString(fragment).toLowerCase(Locale.ROOT);
        if (lowered.isBlank()) {
            return null;
        }

        NodeList nodes = item.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }

            String qualifiedName = defaultString(element.getTagName()).toLowerCase(Locale.ROOT);
            String localName = defaultString(element.getLocalName()).toLowerCase(Locale.ROOT);
            if (!qualifiedName.contains(lowered) && !localName.contains(lowered)) {
                continue;
            }

            String url = firstNonBlank(
                attributeIfImage(element, "url"),
                attributeIfImage(element, "src"),
                attributeIfImage(element, "href")
            );
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    private String extractImageFromChildNodes(Element item) {
        NodeList nodes = item.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element child)) {
                continue;
            }

            String tagName = defaultString(child.getTagName()).toLowerCase(Locale.ROOT);
            if (!tagName.contains("image") && !tagName.contains("thumbnail")) {
                continue;
            }

            String url = firstNonBlank(
                attributeIfImage(child, "url"),
                attributeIfImage(child, "src"),
                cleanHtml(child.getTextContent())
            );
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    private String attributeIfImage(Element element, String attributeName) {
        if (element == null || !StringUtils.hasText(attributeName)) {
            return null;
        }

        String value = element.getAttribute(attributeName);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String type = defaultString(element.getAttribute("type")).toLowerCase(Locale.ROOT);
        if (StringUtils.hasText(type) && !type.contains("image")) {
            return null;
        }

        return value.trim();
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

    private boolean shouldFetchArticlePreview(String sourceUrl, String summary, String title, String imageUrl) {
        if (!newsFeedProperties.isArticlePreviewEnabled() || !StringUtils.hasText(sourceUrl)) {
            return false;
        }
        return !StringUtils.hasText(imageUrl) || isWeakSummary(summary, title);
    }

    private boolean isWeakSummary(String summary, String title) {
        if (!StringUtils.hasText(summary)) {
            return true;
        }

        String normalizedSummary = normalizeForCompare(summary);
        String normalizedTitle = normalizeForCompare(title);
        if (!StringUtils.hasText(normalizedSummary) || normalizedSummary.equals(normalizedTitle)) {
            return true;
        }
        if (normalizedSummary.length() < 40) {
            return true;
        }

        for (String weakPrefix : WEAK_SUMMARY_PREFIXES) {
            if (normalizedSummary.startsWith(weakPrefix)) {
                return true;
            }
        }

        return false;
    }

    private String chooseBestSummary(String currentSummary, String previewSummary, String title) {
        if (!StringUtils.hasText(previewSummary)) {
            return currentSummary;
        }
        if (isWeakSummary(currentSummary, title)) {
            return previewSummary;
        }
        if (previewSummary.length() > currentSummary.length() + 12) {
            return previewSummary;
        }
        return currentSummary;
    }

    private ArticlePreview fetchArticlePreviewSafe(String sourceUrl) {
        try {
            ArticlePreview preview = fetchArticlePreview(sourceUrl);
            if (preview == null) {
                return ArticlePreview.empty();
            }
            return preview;
        } catch (Exception e) {
            return ArticlePreview.empty();
        }
    }

    private ArticlePreview fetchArticlePreview(String sourceUrl) throws Exception {
        URL url = new URL(sourceUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", PREVIEW_USER_AGENT);
        connection.setConnectTimeout(Math.max(1000, newsFeedProperties.getArticlePreviewConnectTimeoutMs()));
        connection.setReadTimeout(Math.max(1000, newsFeedProperties.getArticlePreviewReadTimeoutMs()));

        String contentType = defaultString(connection.getContentType()).toLowerCase(Locale.ROOT);
        if (contentType.contains("application/pdf")) {
            return ArticlePreview.empty();
        }

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] bytes = readLimited(inputStream, Math.max(50000, newsFeedProperties.getArticlePreviewMaxBytes()));
            if (bytes.length == 0) {
                return ArticlePreview.empty();
            }

            String html = new String(bytes, resolveCharset(connection.getContentType()));
            String imageUrl = firstNonBlank(
                resolveAbsoluteUrl(sourceUrl, extractMetaContent(html, "property", "og:image")),
                resolveAbsoluteUrl(sourceUrl, extractMetaContent(html, "name", "twitter:image")),
                resolveAbsoluteUrl(sourceUrl, extractMetaContent(html, "property", "twitter:image"))
            );
            String summary = cleanHtml(firstNonBlank(
                extractMetaContent(html, "property", "og:description"),
                extractMetaContent(html, "name", "description"),
                extractMetaContent(html, "name", "twitter:description"),
                extractFirstParagraph(html)
            ));
            return new ArticlePreview(summary, imageUrl);
        }
    }

    private byte[] readLimited(InputStream stream, int maxBytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = stream.read(buffer)) != -1) {
            int toWrite = Math.min(read, maxBytes - total);
            if (toWrite <= 0) {
                break;
            }
            output.write(buffer, 0, toWrite);
            total += toWrite;
            if (total >= maxBytes) {
                break;
            }
        }
        return output.toByteArray();
    }

    private Charset resolveCharset(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return StandardCharsets.UTF_8;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        int index = lower.indexOf("charset=");
        if (index < 0) {
            return StandardCharsets.UTF_8;
        }
        String value = contentType.substring(index + 8).trim();
        int separator = value.indexOf(';');
        if (separator > 0) {
            value = value.substring(0, separator).trim();
        }
        value = value.replace("\"", "");
        try {
            return Charset.forName(value);
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    private String extractMetaContent(String html, String attribute, String attributeValue) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        Matcher matcher = META_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String attrValue = extractAttribute(tag, attribute);
            if (!attributeValue.equalsIgnoreCase(defaultString(attrValue))) {
                continue;
            }
            String content = extractAttribute(tag, "content");
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        }
        return null;
    }

    private String extractAttribute(String tag, String attribute) {
        Pattern pattern = Pattern.compile(
            "(?i)\\b" + Pattern.quote(attribute) + "\\s*=\\s*(['\"])(.*?)\\1"
        );
        Matcher matcher = pattern.matcher(defaultString(tag));
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    private String extractFirstParagraph(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        String stripped = SCRIPT_BLOCK_PATTERN.matcher(html).replaceAll(" ");
        stripped = STYLE_BLOCK_PATTERN.matcher(stripped).replaceAll(" ");

        Matcher matcher = PARAGRAPH_PATTERN.matcher(stripped);
        while (matcher.find()) {
            String paragraph = cleanHtml(matcher.group(1));
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            String lower = paragraph.toLowerCase(Locale.ROOT);
            if (lower.contains("cookie") || lower.contains("kvkk") || lower.contains("abone")) {
                continue;
            }
            if (paragraph.length() >= 60) {
                return paragraph;
            }
        }
        return null;
    }

    private String resolveAbsoluteUrl(String baseUrl, String candidateUrl) {
        if (!StringUtils.hasText(candidateUrl)) {
            return null;
        }
        String candidate = candidateUrl.trim();
        if (candidate.startsWith("//")) {
            return "https:" + candidate;
        }
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            return candidate;
        }
        if (!StringUtils.hasText(baseUrl)) {
            return candidate;
        }
        try {
            return URI.create(baseUrl).resolve(candidate).toString();
        } catch (Exception ignored) {
            return candidate;
        }
    }

    private String normalizeForCompare(String value) {
        return defaultString(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String normalizeSummary(String summary, String title) {
        String candidate = defaultString(summary);
        String cleanTitle = cleanHtml(defaultString(title));
        if (!StringUtils.hasText(candidate)) {
            candidate = cleanTitle;
        }
        candidate = cleanHtml(candidate);
        if (!StringUtils.hasText(candidate)) {
            candidate = defaultString(cleanTitle).trim();
        }

        String normalized = WHITESPACE_PATTERN.matcher(defaultString(candidate)).replaceAll(" ").trim();
        if (!StringUtils.hasText(normalized)) {
            return defaultString(cleanTitle).trim();
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String suffix : WEAK_SUMMARY_SUFFIXES) {
            if (lower.endsWith(" " + suffix) || lower.endsWith("," + suffix)) {
                return defaultString(cleanTitle).trim();
            }
        }

        if (normalized.length() < 45 && StringUtils.hasText(cleanTitle)) {
            return cleanTitle.trim();
        }
        if (normalized.length() > 360) {
            return normalized.substring(0, 357).trim() + "...";
        }
        return normalized;
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

    private record ArticlePreview(String summary, String imageUrl) {
        private static ArticlePreview empty() {
            return new ArticlePreview(null, null);
        }
    }
}
