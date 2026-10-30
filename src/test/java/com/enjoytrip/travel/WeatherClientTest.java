package com.enjoytrip.travel;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.cache.Cache;
import com.enjoytrip.common.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WeatherClientTest {
  @Test
  void rejectsUnavailableDatesAndNonDomesticCoordinates() {
    LocalDate today = LocalDate.now(WeatherClient.SEOUL);
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeatherClient.Query(37, 127, today.minusDays(1), today));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeatherClient.Query(37, 127, today, today.plusDays(16)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeatherClient.Query(Double.NaN, 127, today, today));
  }

  @Test
  void parsesNullsAndKoreanTimesAndCachesSuccessfulResults() throws Exception {
    LocalDate today = LocalDate.now(WeatherClient.SEOUL);
    AtomicInteger calls = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        ex -> {
          calls.incrementAndGet();
          assertTrue(ex.getRequestURI().getRawQuery().contains("timezone=Asia%2FSeoul"));
          byte[] data =
              ("{\"daily\":{\"time\":[\""
                      + today
                      + "\"],\"weather_code\":[61],\"temperature_2m_min\":[null],\"temperature_2m_max\":[20.5],\"precipitation_probability_max\":[80],\"sunrise\":[\""
                      + today
                      + "T06:20\"],\"sunset\":[\""
                      + today
                      + "T18:40\"]}}")
                  .getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(200, data.length);
          ex.getResponseBody().write(data);
          ex.close();
        });
    server.start();
    try {
      Map<String, String> values = new HashMap<>();
      Cache cache =
          new Cache() {
            public Optional<String> get(String k) {
              return Optional.ofNullable(values.get(k));
            }

            public void put(String k, String v, int ttl) {
              assertEquals(1800, ttl);
              values.put(k, v);
            }
          };
      var client =
          new WeatherClient(
              new Config(
                  Map.of("weather.base-url", "http://127.0.0.1:" + server.getAddress().getPort())),
              new JsonApi(cache));
      var query = new WeatherClient.Query(37.5, 127, today, today);
      var day = client.forecast(query).days().get(0);
      assertEquals("비", day.condition());
      assertEquals("정보 없음", day.minimum());
      assertEquals("20.5", day.maximum());
      assertEquals("06:20", day.sunrise());
      client.forecast(query);
      assertEquals(1, calls.get());
      client.forecast(new WeatherClient.Query(35.1, 129, today, today));
      assertEquals(2, calls.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void doesNotCacheGatewayErrorsOrExposeCredentials() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/",
        ex -> {
          calls.incrementAndGet();
          byte[] data =
              "{\"error\":true,\"reason\":\"secret-key\"}".getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(200, data.length);
          ex.getResponseBody().write(data);
          ex.close();
        });
    server.start();
    try {
      Cache cache =
          new Cache() {
            public Optional<String> get(String k) {
              return Optional.empty();
            }

            public void put(String k, String v, int ttl) {
              fail("error cached");
            }
          };
      var api = new JsonApi(cache);
      for (int i = 0; i < 2; i++) {
        var e =
            assertThrows(
                JsonApi.Failure.class,
                () ->
                    api.get(
                        "예보",
                        URI.create(
                            "http://127.0.0.1:"
                                + server.getAddress().getPort()
                                + "/?serviceKey=secret-key"),
                        1800,
                        n -> !n.path("error").asBoolean()));
        assertFalse(e.getMessage().contains("secret-key"));
        assertNull(e.getCause());
      }
      assertEquals(2, calls.get());
    } finally {
      server.stop(0);
    }
  }
}
