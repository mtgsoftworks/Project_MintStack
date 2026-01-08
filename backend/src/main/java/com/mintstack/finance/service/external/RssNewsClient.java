package com.mintstack.finance.service.external;

import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.repository.NewsCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssNewsClient {

    private final NewsCategoryRepository categoryRepository;

    // RSS Feed URLs for Turkish financial news
    private static final Map<String, String> RSS_FEEDS = Map.of(
        "genel-ekonomi", "https://www.bloomberght.com/rss",
        "hisse-senedi", "https://www.bloomberght.com/borsa/rss",
        "doviz", "https://www.bloomberght.com/doviz/rss",
        "dunya-ekonomisi", "https://www.bloomberght.com/piyasa/rss"
    );

    // Backup RSS feeds
    private static final Map<String, String> BACKUP_RSS_FEEDS = Map.of(
        "genel-ekonomi", "https://www.haberturk.com/rss/ekonomi.xml",
        "hisse-senedi", "https://www.haberturk.com/rss/ekonomi.xml"
    );

    /**
     * Fetch news from all RSS feeds
     */
    public List<News> fetchAllNews() {
        List<News> allNews = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : RSS_FEEDS.entrySet()) {
            try {
                List<News> categoryNews = fetchNewsFromRss(entry.getValue(), entry.getKey());
                allNews.addAll(categoryNews);
                log.info("Fetched {} news from {} category", categoryNews.size(), entry.getKey());
            } catch (Exception e) {
                log.warn("Failed to fetch news from {}: {}", entry.getKey(), e.getMessage());
                // Try backup feed
                String backupUrl = BACKUP_RSS_FEEDS.get(entry.getKey());
                if (backupUrl != null) {
                    try {
                        List<News> backupNews = fetchNewsFromRss(backupUrl, entry.getKey());
                        allNews.addAll(backupNews);
                        log.info("Fetched {} news from backup for {}", backupNews.size(), entry.getKey());
                    } catch (Exception ex) {
                        log.error("Backup feed also failed for {}", entry.getKey());
                    }
                }
            }
        }
        
        return allNews;
    }

    /**
     * Fetch news from a specific RSS feed
     */
    public List<News> fetchNewsFromRss(String rssUrl, String categorySlug) {
        List<News> newsList = new ArrayList<>();
        
        try {
            NewsCategory category = categoryRepository.findBySlug(categorySlug).orElse(null);
            if (category == null) {
                log.warn("Category not found: {}", categorySlug);
                return newsList;
            }

            Document doc = fetchRssDocument(rssUrl);
            NodeList items = doc.getElementsByTagName("item");
            
            for (int i = 0; i < Math.min(items.getLength(), 10); i++) { // Max 10 news per category
                Element item = (Element) items.item(i);
                
                News news = News.builder()
                    .title(getElementText(item, "title"))
                    .summary(cleanHtml(getElementText(item, "description")))
                    .content(cleanHtml(getElementText(item, "description")))
                    .sourceUrl(getElementText(item, "link"))
                    .sourceName(extractSourceName(rssUrl))
                    .imageUrl(extractImageUrl(item))
                    .category(category)
                    .publishedAt(parseDate(getElementText(item, "pubDate")))
                    .isPublished(true)
                    .viewCount(0L)
                    .build();
                
                if (news.getTitle() != null && !news.getTitle().isEmpty()) {
                    newsList.add(news);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing RSS feed {}: {}", rssUrl, e.getMessage());
        }
        
        return newsList;
    }

    private Document fetchRssDocument(String rssUrl) throws Exception {
        URL url = new URL(rssUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "MintStack Finance Bot/1.0");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        
        try (InputStream inputStream = connection.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(inputStream);
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private String cleanHtml(String html) {
        if (html == null) return null;
        // Remove HTML tags and decode entities
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .trim();
    }

    private String extractSourceName(String url) {
        if (url.contains("bloomberght")) return "Bloomberg HT";
        if (url.contains("haberturk")) return "Habertürk";
        if (url.contains("dunya")) return "Dünya Gazetesi";
        if (url.contains("aa.com")) return "Anadolu Ajansı";
        return "Finans Haberleri";
    }

    private String extractImageUrl(Element item) {
        // Try to get image from enclosure tag
        NodeList enclosures = item.getElementsByTagName("enclosure");
        if (enclosures.getLength() > 0) {
            Element enclosure = (Element) enclosures.item(0);
            String type = enclosure.getAttribute("type");
            if (type != null && type.startsWith("image")) {
                return enclosure.getAttribute("url");
            }
        }
        
        // Try media:content
        NodeList mediaContent = item.getElementsByTagName("media:content");
        if (mediaContent.getLength() > 0) {
            return ((Element) mediaContent.item(0)).getAttribute("url");
        }
        
        // Try media:thumbnail
        NodeList mediaThumbnail = item.getElementsByTagName("media:thumbnail");
        if (mediaThumbnail.getLength() > 0) {
            return ((Element) mediaThumbnail.item(0)).getAttribute("url");
        }
        
        return null;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        
        try {
            // RSS date format: "Mon, 06 Jan 2025 10:30:00 +0300"
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                // Alternative format
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                return LocalDateTime.parse(dateStr, altFormatter);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }
}
