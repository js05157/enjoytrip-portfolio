package com.enjoytrip.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.attraction.*;
import com.enjoytrip.common.Config;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachedAttractionServiceTest {
  static class Store implements Cache {
    Map<String, String> values = new HashMap<>();
    int ttl;

    public Optional<String> get(String key) {
      return Optional.ofNullable(values.get(key));
    }

    public void put(String key, String value, int ttl) {
      values.put(key, value);
      this.ttl = ttl;
    }
  }

  @Test
  void cachesRoundTripAndSeparatesEveryQueryParameter() {
    Store store = new Store();
    AtomicInteger calls = new AtomicInteger();
    TourClient source =
        new TourClient(new Config()) {
          @Override
          public Result search(AttractionQuery q) {
            calls.incrementAndGet();
            return super.search(q);
          }
        };
    CachedAttractionService service = new CachedAttractionService(source, store, 600, "test");
    var q = new AttractionQuery("11", "", "12", "", 1, 12, "A");
    assertEquals(service.search(q), service.search(q));
    assertEquals(1, calls.get());
    assertEquals(600, store.ttl);
    assertEquals(1, service.metrics().get("hits"));
    for (var next :
        List.of(
            new AttractionQuery("26", "", "12", "", 1, 12, "A"),
            new AttractionQuery("11", "1", "12", "", 1, 12, "A"),
            new AttractionQuery("11", "", "14", "", 1, 12, "A"),
            new AttractionQuery("11", "", "12", "서울", 1, 12, "A"),
            new AttractionQuery("11", "", "12", "", 2, 12, "A"),
            new AttractionQuery("11", "", "12", "", 1, 6, "A"),
            new AttractionQuery("11", "", "12", "", 1, 12, "D")))
      assertNotEquals(service.key(q), service.key(next));
  }

  @Test
  void corruptedOrMissingCacheFallsBackToSource() {
    Store store = new Store();
    var service = new CachedAttractionService(new TourClient(new Config()), store, 600, "test");
    var q = new AttractionQuery("11", "", "12", "", 1, 12, "A");
    store.values.put(service.key(q), "broken-json");
    assertEquals(1, service.search(q).total());
    assertTrue(store.values.get(service.key(q)).contains("items"));
  }
}
