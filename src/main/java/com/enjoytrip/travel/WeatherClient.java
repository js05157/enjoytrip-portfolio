package com.enjoytrip.travel;

import com.enjoytrip.common.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.*;
import java.util.*;

public final class WeatherClient {
  public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final Config config;
  private final JsonApi api;

  public WeatherClient(Config config, JsonApi api) {
    this.config = config;
    this.api = api;
  }

  public record Query(double latitude, double longitude, LocalDate start, LocalDate end) {
    public Query {
      LocalDate today = LocalDate.now(SEOUL);
      if (!Double.isFinite(latitude)
          || !Double.isFinite(longitude)
          || latitude < 32
          || latitude > 40
          || longitude < 124
          || longitude > 132)
        throw new IllegalArgumentException("국내 여행지 좌표를 입력하세요 (위도 32~40, 경도 124~132).");
      if (start == null
          || end == null
          || start.isBefore(today)
          || end.isBefore(start)
          || end.isAfter(today.plusDays(15)))
        throw new IllegalArgumentException("예보는 오늘부터 15일 뒤까지 날짜 순서대로 조회할 수 있습니다.");
    }
  }

  public record Day(
      String date,
      String condition,
      String minimum,
      String maximum,
      String rain,
      String sunrise,
      String sunset) {
    public String getDate() {
      return date;
    }

    public String getCondition() {
      return condition;
    }

    public String getMinimum() {
      return minimum;
    }

    public String getMaximum() {
      return maximum;
    }

    public String getRain() {
      return rain;
    }

    public String getSunrise() {
      return sunrise;
    }

    public String getSunset() {
      return sunset;
    }
  }

  public record Forecast(List<Day> days, String retrievedAt) {
    public List<Day> getDays() {
      return days;
    }

    public String getRetrievedAt() {
      return retrievedAt;
    }
  }

  public Forecast forecast(Query q) {
    String url =
        config.get("weather.base-url")
            + "?latitude="
            + q.latitude()
            + "&longitude="
            + q.longitude()
            + "&daily=weather_code,temperature_2m_min,temperature_2m_max,precipitation_probability_max,sunrise,sunset"
            + "&timezone=Asia%2FSeoul&start_date="
            + q.start()
            + "&end_date="
            + q.end();
    JsonNode root =
        api.get(
            "날씨",
            URI.create(url),
            1800,
            n -> !n.path("error").asBoolean() && n.path("daily").path("time").isArray());
    JsonNode d = root.path("daily");
    List<Day> days = new ArrayList<>();
    int count = (int) java.time.temporal.ChronoUnit.DAYS.between(q.start(), q.end()) + 1;
    if (d.path("time").size() != count) throw new JsonApi.Failure("요청한 기간의 예보가 아직 제공되지 않습니다.");
    for (int i = 0; i < count; i++) {
      String date = d.path("time").path(i).asText();
      if (!date.equals(q.start().plusDays(i).toString()))
        throw new JsonApi.Failure("예보 날짜가 요청한 기간과 다릅니다.");
      days.add(
          new Day(
              date,
              condition(d.path("weather_code").path(i)),
              number(d, "temperature_2m_min", i),
              number(d, "temperature_2m_max", i),
              number(d, "precipitation_probability_max", i),
              time(d, "sunrise", i),
              time(d, "sunset", i)));
    }
    return new Forecast(
        List.copyOf(days), LocalDateTime.now(SEOUL).withNano(0).toString().replace('T', ' '));
  }

  private static String number(JsonNode d, String key, int i) {
    JsonNode n = d.path(key).path(i);
    return n.isNumber() ? n.asText() : "정보 없음";
  }

  private static String time(JsonNode d, String key, int i) {
    try {
      return LocalDateTime.parse(d.path(key).path(i).asText()).toLocalTime().toString();
    } catch (Exception e) {
      return "정보 없음";
    }
  }

  private static String condition(JsonNode n) {
    if (!n.isNumber()) return "정보 없음";
    return switch (n.asInt()) {
      case 0 -> "맑음";
      case 1 -> "대체로 맑음";
      case 2 -> "구름 조금";
      case 3 -> "흐림";
      case 45, 48 -> "안개";
      case 51, 53, 55 -> "이슬비";
      case 56, 57, 66, 67 -> "어는 비";
      case 61, 63, 65 -> "비";
      case 71, 73, 75, 77 -> "눈";
      case 80, 81, 82 -> "소나기";
      case 85, 86 -> "눈 소나기";
      case 95, 96, 99 -> "뇌우";
      default -> "정보 없음";
    };
  }
}
