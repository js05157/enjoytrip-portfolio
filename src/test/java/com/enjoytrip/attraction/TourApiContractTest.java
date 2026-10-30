package com.enjoytrip.attraction;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.common.Config;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TourApiContractTest {
  @Test
  void callsVersionTwoEndpointsEncodesKeysAndHandlesSingleItem() throws Exception {
    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> query = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          query.set(exchange.getRequestURI().getRawQuery());
          String body =
              "{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"totalCount\":1,\"items\":{\"item\":{\"contentid\":\"42\",\"title\":\"서울"
                  + " 관광지\",\"contenttypeid\":\"12\",\"areacode\":\"1\",\"mapy\":\"37.5\",\"mapx\":\"127.0\",\"overview\":\"소개&lt;br&gt;\"}}}}}";
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      TourClient client =
          new TourClient(
              new Config(
                  Map.of(
                      "app.mode",
                      "live",
                      "tour.base-url",
                      "http://127.0.0.1:" + server.getAddress().getPort(),
                      "tour.service-key",
                      "a+b/c=")));
      var result = client.search(new AttractionQuery("11", "110", "12", "", 1, 12, "A"));
      assertEquals(1, result.total());
      assertEquals("서울 관광지", result.items().get(0).title());
      assertEquals("/areaBasedList2", path.get());
      assertTrue(query.get().contains("serviceKey=a%2Bb%2Fc%3D"));
      assertTrue(query.get().contains("lDongRegnCd=11"));
      assertTrue(query.get().contains("lDongSignguCd=110"));
      assertFalse(query.get().contains("areaCode="));
      assertFalse(query.get().contains("sigunguCode="));
      client.search(new AttractionQuery("11", "", "12", "서울", 1, 12, "A"));
      assertEquals("/searchKeyword2", path.get());
      assertNotNull(client.detail("42"));
      assertEquals("/detailCommon2", path.get());
      client.regions();
      assertEquals("/ldongCode2", path.get());
    } finally {
      server.stop(0);
    }
  }
}
