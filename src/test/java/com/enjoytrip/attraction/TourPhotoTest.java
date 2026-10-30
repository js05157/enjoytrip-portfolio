package com.enjoytrip.attraction;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.common.Config;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TourPhotoTest {
  @Test
  void retrievesOriginalPhotosWithRightsAndNoObsoleteParameters() throws Exception {
    AtomicReference<String> query = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/detailImage2",
        ex -> {
          query.set(ex.getRequestURI().getRawQuery());
          byte[] data =
              "{\"response\":{\"header\":{\"resultCode\":\"0000\"},\"body\":{\"items\":{\"item\":[{\"originimgurl\":\"http://tong.visitkorea.or.kr/cms/photo.jpg\",\"imgname\":\"서울\",\"cpyrhtDivCd\":\"Type3\"},{\"originimgurl\":\"javascript:alert(1)\"}]}}}}"
                  .getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(200, data.length);
          ex.getResponseBody().write(data);
          ex.close();
        });
    server.start();
    try {
      var client =
          new TourClient(
              new Config(
                  Map.of(
                      "app.mode",
                      "live",
                      "tour.base-url",
                      "http://127.0.0.1:" + server.getAddress().getPort(),
                      "tour.service-key",
                      "secret")));
      var photos = client.photos("126128");
      assertEquals(1, photos.size());
      assertTrue(photos.get(0).url().startsWith("https://"));
      assertTrue(photos.get(0).copyright().contains("변경 금지"));
      assertFalse(query.get().contains("subImageYN"));
      assertFalse(query.get().contains("imageYN"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void reportsGatewayOperationAndCodeWithoutCredential() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        ex -> {
          byte[] data =
              "{\"OpenAPI_ServiceResponse\":{\"cmmMsgHeader\":{\"returnReasonCode\":\"30\",\"returnAuthMsg\":\"secret\"}}}"
                  .getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(403, data.length);
          ex.getResponseBody().write(data);
          ex.close();
        });
    server.start();
    try {
      var client =
          new TourClient(
              new Config(
                  Map.of(
                      "app.mode",
                      "live",
                      "tour.base-url",
                      "http://127.0.0.1:" + server.getAddress().getPort(),
                      "tour.service-key",
                      "secret")));
      var e = assertThrows(TourClient.ApiFailure.class, () -> client.photos("126128"));
      assertTrue(e.getMessage().contains("detailImage2"));
      assertTrue(e.getMessage().contains("30"));
      assertFalse(e.getMessage().contains("secret"));
    } finally {
      server.stop(0);
    }
  }
}
