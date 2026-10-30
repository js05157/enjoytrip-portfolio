package com.enjoytrip.common;

import com.enjoytrip.cache.Cache;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Predicate;

/** 정상 응답만 캐시하며 인증키가 포함된 URL과 본문은 오류에 노출하지 않는다. */
public final class JsonApi {
  private final Cache cache;
  private final ObjectMapper json = new ObjectMapper();
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

  public JsonApi(Cache cache) {
    this.cache = cache;
  }

  public JsonNode get(String name, URI uri, int ttl, Predicate<JsonNode> valid) {
    try {
      String key =
          "enjoytrip:v1:external:"
              + name
              + ":"
              + HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(uri.toString().getBytes(StandardCharsets.UTF_8)));
      var cached = cache.get(key);
      if (cached.isPresent()) {
        try {
          JsonNode value = json.readTree(cached.get());
          if (value != null && valid.test(value)) return value;
        } catch (Exception ignored) {
        }
      }
      var response =
          http.send(
              HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build(),
              HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200)
        throw new Failure(
            name + " 제공 서버가 응답하지 않습니다 (HTTP " + response.statusCode() + "). 잠시 후 다시 조회해 주세요.");
      JsonNode value = json.readTree(response.body());
      if (value == null || !valid.test(value))
        throw new Failure(name + " 응답을 확인할 수 없습니다. 서비스 활용 등록과 제공 상태를 확인해 주세요.");
      cache.put(key, response.body(), ttl);
      return value;
    } catch (Failure e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new Failure(name + " 요청이 중단되었습니다.");
    } catch (Exception e) {
      throw new Failure(name + " 정보를 불러오지 못했습니다. 잠시 후 다시 조회해 주세요.");
    }
  }

  public static final class Failure extends IllegalStateException {
    public Failure(String message) {
      super(message);
    }
  }
}
