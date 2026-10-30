package com.enjoytrip.news;

import com.enjoytrip.common.Database;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class NewsService {
  public record Article(String title, String url, String source, String publishedAt) {}

  private final Database db;
  private final AtomicLong nextCollection = new AtomicLong();

  public NewsService(Database db) {
    this.db = db;
  }

  public List<Article> list(int page) {
    try (Connection c = db.connection();
        PreparedStatement s =
            c.prepareStatement("SELECT * FROM news_articles ORDER BY id DESC LIMIT 20 OFFSET ?")) {
      s.setInt(1, (page - 1) * 20);
      List<Article> result = new ArrayList<>();
      try (ResultSet r = s.executeQuery()) {
        while (r.next())
          result.add(
              new Article(
                  r.getString("title"),
                  r.getString("url"),
                  r.getString("source"),
                  r.getString("published_at")));
      }
      return result;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  public int collect() {
    long now = System.currentTimeMillis(), next = nextCollection.get();
    if (now < next || !nextCollection.compareAndSet(next, now + 300_000))
      throw new IllegalArgumentException("뉴스는 5분에 한 번 수집할 수 있습니다.");
    String url =
        "https://news.google.com/rss/search?q="
            + java.net.URLEncoder.encode("국내 관광 여행", StandardCharsets.UTF_8)
            + "&hl=ko&gl=KR&ceid=KR:ko";
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(),
              HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        if (response.statusCode() != 200) throw new IOException();
        byte[] bytes = body.readNBytes(1_000_001);
        if (bytes.length > 1_000_000) throw new IOException();
        int count = 0;
        for (Article article : parse(bytes)) if (save(article)) count++;
        return count;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("뉴스 수집이 중단되었습니다.");
    } catch (Exception e) {
      throw new IllegalStateException("뉴스를 수집하지 못했습니다. 잠시 후 다시 시도하세요.");
    }
  }

  public static List<Article> parse(byte[] bytes) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setFeature("http://xml.org/sax/features/external-general-entities", false);
    f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    f.setXIncludeAware(false);
    f.setExpandEntityReferences(false);
    Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    NodeList items = doc.getElementsByTagName("item");
    List<Article> rows = new ArrayList<>();
    for (int i = 0; i < Math.min(100, items.getLength()); i++) {
      Element item = (Element) items.item(i);
      String link = text(item, "link", 1500), title = text(item, "title", 300);
      try {
        URI uri = URI.create(link);
        if (!"https".equals(uri.getScheme())
            || uri.getHost() == null
            || uri.getUserInfo() != null
            || title.isBlank()) continue;
      } catch (Exception e) {
        continue;
      }
      rows.add(new Article(title, link, text(item, "source", 150), text(item, "pubDate", 100)));
    }
    return rows;
  }

  private static String text(Element e, String name, int max) {
    Node n = e.getElementsByTagName(name).item(0);
    String value = n == null ? "" : n.getTextContent().trim();
    return value.substring(0, Math.min(value.length(), max));
  }

  public boolean save(Article a) {
    try (Connection c = db.connection();
        PreparedStatement s =
            c.prepareStatement(
                "INSERT INTO news_articles(url_hash,title,url,source,published_at)"
                    + " VALUES(?,?,?,?,?)")) {
      s.setString(
          1,
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(a.url().getBytes(StandardCharsets.UTF_8))));
      s.setString(2, a.title());
      s.setString(3, a.url());
      s.setString(4, a.source());
      s.setString(5, a.publishedAt());
      s.executeUpdate();
      return true;
    } catch (SQLException e) {
      if (e.getSQLState() != null && e.getSQLState().startsWith("23")) return false;
      throw new IllegalStateException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
