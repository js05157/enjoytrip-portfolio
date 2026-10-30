package com.enjoytrip.travel;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.cache.Cache;
import com.enjoytrip.common.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChargerClientTest {
  @Test
  void parsesSingleChargerCoordinatesStatusAndCachesForOneMinute() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/getChargerInfo",
        ex -> {
          calls.incrementAndGet();
          String q = ex.getRequestURI().getRawQuery();
          assertTrue(q.contains("ServiceKey=a%2Bb%3D"));
          assertTrue(q.contains("zcode=11"));
          byte[] data =
              "{\"resultCode\":\"00\",\"totalCount\":1,\"items\":{\"item\":{\"statId\":\"S1\",\"statNm\":\"서울 충전소\",\"chgerId\":\"01\",\"chgerType\":\"04\",\"addr\":\"서울\",\"lat\":\"37.5\",\"lng\":\"127\",\"stat\":\"2\",\"statUpdDt\":\"20261030130000\",\"parkingFree\":\"Y\"}}}"
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
              assertFalse(k.contains("a+b="));
              return Optional.ofNullable(values.get(k));
            }

            public void put(String k, String v, int ttl) {
              assertEquals(60, ttl);
              values.put(k, v);
            }
          };
      var client =
          new ChargerClient(
              new Config(
                  Map.of(
                      "ev.service-key",
                      "a+b=",
                      "ev.base-url",
                      "http://127.0.0.1:" + server.getAddress().getPort())),
              new JsonApi(cache));
      var result = client.search(new ChargerClient.Query("11", 1));
      assertEquals(1, result.total());
      var c = result.items().get(0);
      assertEquals("충전 대기", c.status());
      assertEquals("무료", c.parking());
      assertEquals("DC 콤보", c.type());
      assertTrue(c.getHasCoordinates());
      assertEquals("2026-10-30 13:00", c.updated());
      client.search(new ChargerClient.Query("11", 1));
      assertEquals(1, calls.get());
      client.search(new ChargerClient.Query("11", 2));
      assertEquals(2, calls.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsInvalidQueriesAndMissingPersonalConfiguration() {
    assertThrows(IllegalArgumentException.class, () -> new ChargerClient.Query("11&x=1", 1));
    assertThrows(IllegalArgumentException.class, () -> new ChargerClient.Query("11", 0));
    var c = new ChargerClient(new Config(Map.of("ev.service-key", "")), null);
    assertFalse(c.configured());
    assertThrows(JsonApi.Failure.class, () -> c.search(new ChargerClient.Query("11", 1)));
  }
}
