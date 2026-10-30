package com.enjoytrip.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import redis.clients.jedis.*;

@EnabledIfEnvironmentVariable(named = "REDIS_TEST_URI", matches = ".+")
class RedisIntegrationTest {
  @Test
  void redisStoresAndExpiresValues() throws Exception {
    try (Jedis jedis = new Jedis(URI.create(System.getenv("REDIS_TEST_URI")))) {
      String key = "enjoytrip:test:" + UUID.randomUUID();
      jedis.setex(key, 1, "한글 데이터");
      assertEquals("한글 데이터", jedis.get(key));
      assertTrue(jedis.ttl(key) > 0);
      Thread.sleep(1100);
      assertNull(jedis.get(key));
    }
  }
}
