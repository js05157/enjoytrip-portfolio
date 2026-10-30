package com.enjoytrip.cache;
import com.enjoytrip.common.Config;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;
import java.net.URI;
import java.util.Optional;
import java.util.logging.Logger;
public final class RedisCache implements Cache {
 private final JedisPool pool;
 private volatile long retryAfter;
 private static final Logger LOG=Logger.getLogger(RedisCache.class.getName());
 public RedisCache(Config config){pool=config.enabled("redis.enabled")?new JedisPool(URI.create(config.require("redis.uri")),300):null;}
 @Override public Optional<String> get(String key){
  if(pool==null||System.nanoTime()<retryAfter)return Optional.empty();
  try(Jedis jedis=pool.getResource()){return Optional.ofNullable(jedis.get(key));}catch(RuntimeException e){failed();return Optional.empty();}
 }
 @Override public void put(String key,String value,int seconds){
  if(pool==null||System.nanoTime()<retryAfter)return;
  try(Jedis jedis=pool.getResource()){jedis.set(key,value,SetParams.setParams().ex(seconds));}catch(RuntimeException e){failed();}
 }
 private synchronized void failed(){if(System.nanoTime()>=retryAfter)LOG.warning("Redis 연결 실패: 잠시 캐시를 건너뛰고 원본 데이터를 조회합니다.");retryAfter=System.nanoTime()+5_000_000_000L;}
 @Override public boolean available(){if(pool==null)return false;try(Jedis j=pool.getResource()){return "PONG".equals(j.ping());}catch(RuntimeException e){return false;}}
 @Override public void close(){if(pool!=null)pool.close();}
}
