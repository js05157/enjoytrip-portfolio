package com.enjoytrip.cache;
import com.enjoytrip.attraction.*;
import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
public final class CachedAttractionService extends AttractionService {
 private final Cache cache;private final int ttl;private final String namespace;
 private final ObjectMapper json=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
 private final LongAdder hits=new LongAdder(),misses=new LongAdder();
 public CachedAttractionService(TourClient source,Cache cache,int ttl,String namespace){super(source);if(ttl<1||ttl>86400)throw new IllegalArgumentException("캐시 TTL은 1~86400초여야 합니다.");this.cache=cache;this.ttl=ttl;this.namespace=namespace;}
 public String key(AttractionQuery query){
  try {String identity=json.writeValueAsString(query);return "enjoytrip:v1:"+namespace+":attractions:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException(e);}
 }
 @Override public TourClient.Result search(AttractionQuery query){
  String key=key(query);Optional<String> value=cache.get(key);
  if(value.isPresent())try{TourClient.Result result=json.readValue(value.get(),TourClient.Result.class);if(result.items()!=null&&result.total()>=0){hits.increment();return result;}}catch(Exception ignored){/* 손상된 캐시는 원본으로 복구한다. */}
  misses.increment();TourClient.Result result=source.search(query);
  try{cache.put(key,json.writeValueAsString(result),ttl);}catch(com.fasterxml.jackson.core.JsonProcessingException ignored){}
  return result;
 }
 public Map<String,Long> metrics(){return Map.of("hits",hits.sum(),"misses",misses.sum());}
}
