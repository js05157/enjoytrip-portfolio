package com.enjoytrip.cache;
import java.util.Optional;
public interface Cache extends AutoCloseable {
 Optional<String> get(String key);
 void put(String key,String value,int seconds);
 default boolean available(){return false;}
 default void close(){}
}
