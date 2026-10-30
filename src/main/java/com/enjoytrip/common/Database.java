package com.enjoytrip.common;

import com.zaxxer.hikari.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;

public final class Database implements AutoCloseable {
  private final HikariDataSource pool;

  public Database(Config config) {
    String url = config.get("db.url");
    if (url.isBlank()) {
      if (!config.demo()) throw new IllegalStateException("live 모드에는 MySQL 설정이 필요합니다.");
      Path dir =
          Path.of(System.getProperty("catalina.base", "."), "enjoytrip-data").toAbsolutePath();
      try {
        Files.createDirectories(dir);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      url =
          "jdbc:h2:file:"
              + dir.resolve("enjoytrip").toString().replace('\\', '/')
              + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    }
    HikariConfig settings = new HikariConfig();
    settings.setJdbcUrl(url);
    settings.setUsername(config.get("db.user"));
    settings.setPassword(config.get("db.password"));
    settings.setDriverClassName(
        url.startsWith("jdbc:h2:") ? "org.h2.Driver" : "com.mysql.cj.jdbc.Driver");
    settings.setMaximumPoolSize(8);
    settings.setMinimumIdle(1);
    settings.setConnectionTimeout(5000);
    pool = new HikariDataSource(settings);
    if (config.enabled("db.initialize")) initialize();
  }

  public Connection connection() throws SQLException {
    return pool.getConnection();
  }

  private void initialize() {
    try (InputStream input = getClass().getResourceAsStream("/db/schema.sql")) {
      if (input == null) throw new IllegalStateException("스키마가 없습니다.");
      String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      try (Connection c = connection();
          Statement s = c.createStatement()) {
        for (String part : sql.split(";")) if (!part.isBlank()) s.execute(part);
      }
    } catch (IOException | SQLException e) {
      throw new IllegalStateException("DB 초기화 실패", e);
    }
  }

  public void close() {
    pool.close();
  }
}
