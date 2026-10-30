package com.enjoytrip.common;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.cache.Cache;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JsonApiTest {
  @Test
  void reportsHttpAndNumericGatewayCodeWithoutExposingRequestOrResponseSecrets() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        ex -> {
          byte[] body =
              "{\"OpenAPI_ServiceResponse\":{\"cmmMsgHeader\":{\"returnReasonCode\":\"30\",\"returnAuthMsg\":\"secret-key\"}}}"
                  .getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(403, body.length);
          ex.getResponseBody().write(body);
          ex.close();
        });
    server.start();
    try {
      Cache cache =
          new Cache() {
            public Optional<String> get(String key) {
              return Optional.empty();
            }

            public void put(String key, String value, int ttl) {
              fail("Failed responses must not be cached");
            }
          };
      var error =
          assertThrows(
              JsonApi.Failure.class,
              () ->
                  new JsonApi(cache)
                      .get(
                          "충전소",
                          URI.create(
                              "http://127.0.0.1:"
                                  + server.getAddress().getPort()
                                  + "/?serviceKey=secret-key"),
                          60,
                          n -> true));
      assertTrue(error.getMessage().contains("HTTP 403"));
      assertTrue(error.getMessage().contains("오류코드 30"));
      assertFalse(error.getMessage().contains("secret-key"));
      assertFalse(error.getMessage().contains("serviceKey="));
      assertNull(error.getCause());
    } finally {
      server.stop(0);
    }
  }
}
