package com.enjoytrip.attraction;
import com.enjoytrip.common.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TourClientTest {
 @Test void filtersByRegionContentAndKeyword() {
  TourClient client=new TourClient(new Config());
  var result=client.search(new AttractionQuery("1","","12","서울",1,12,"A"));
  assertEquals(1,result.total());assertEquals("12",result.items().get(0).contentTypeId());assertTrue(result.items().get(0).title().startsWith("[샘플]"));
 }
 @Test void normalizesEmptyAndSingleApiItems() throws Exception {
  var json=new ObjectMapper();
  assertTrue(TourClient.items(json.readTree("{\"items\":\"\"}")).isEmpty());
  assertEquals(1,TourClient.items(json.readTree("{\"items\":{\"item\":{\"title\":\"서울\"}}}")).size());
 }
 @Test void rejectsInvalidQueries() {
  assertThrows(IllegalArgumentException.class,()->new AttractionQuery("","1","12","",1,12,"A"));
  assertThrows(IllegalArgumentException.class,()->new AttractionQuery("1","","99","",1,12,"A"));
 }
}
