package com.enjoytrip.common;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class Config {
    private final Properties values = new Properties();
    public Config() {
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            if (in != null) values.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            String file = System.getProperty("enjoytrip.config", System.getenv().getOrDefault("APP_CONFIG", ""));
            if (!file.isBlank()) try (Reader reader = Files.newBufferedReader(Path.of(file))) { values.load(reader); }
        } catch (IOException e) { throw new IllegalStateException("설정 파일을 읽을 수 없습니다.", e); }
    }
    public String get(String key) { return System.getenv().getOrDefault(key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_'), values.getProperty(key, "")); }
    public boolean demo() { return "demo".equals(get("app.mode")); }
    public boolean enabled(String key) { return Boolean.parseBoolean(get(key)); }
    public int number(String key) { return Integer.parseInt(get(key)); }
    public String require(String key) {
        String value = get(key);
        if (value.isBlank()) throw new IllegalStateException("개인 설정이 필요합니다: " + key);
        return value;
    }
}
