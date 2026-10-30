package com.enjoytrip.news;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class NewsServiceTest {
 @Test void parsesHeadlinesAndRejectsUnsafeLinks()throws Exception{String xml="<rss><channel><item><title>여행 소식</title><link>https://example.com/travel</link><source>매체</source></item><item><title>잘못된 링크</title><link>javascript:alert(1)</link></item></channel></rss>";var articles=NewsService.parse(xml.getBytes(StandardCharsets.UTF_8));assertEquals(1,articles.size());assertEquals("여행 소식",articles.get(0).title());}
 @Test void rejectsExternalEntities(){assertThrows(Exception.class,()->NewsService.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///missing'>]><rss>&x;</rss>".getBytes(StandardCharsets.UTF_8)));}
}
