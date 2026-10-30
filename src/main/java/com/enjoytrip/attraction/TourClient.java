package com.enjoytrip.attraction;

import com.enjoytrip.common.*;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class TourClient {
  public static final Map<String, String> TYPES = types();

  private static Map<String, String> types() {
    Map<String, String> values = new LinkedHashMap<>();
    String[] codes = {"12", "14", "15", "25", "28", "32", "38", "39"};
    String[] names = {"관광지", "문화시설", "축제·공연", "여행코스", "레포츠", "숙박", "쇼핑", "음식점"};
    for (int i = 0; i < codes.length; i++) values.put(codes[i], names[i]);
    return Collections.unmodifiableMap(values);
  }

  public static final Map<String, String> REGIONS = new LinkedHashMap<>();

  static {
    String[] codes = {
      "11", "26", "27", "28", "29", "30", "31", "36", "41", "43", "44", "46", "47", "48", "50",
      "51", "52"
    };
    String[] names = {
      "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "충북", "충남", "전남", "경북", "경남", "제주",
      "강원", "전북"
    };
    for (int i = 0; i < codes.length; i++) REGIONS.put(codes[i], names[i]);
  }

  public record Result(List<Attraction> items, int total) {
    public List<Attraction> getItems() {
      return items;
    }

    public int getTotal() {
      return total;
    }
  }

  public record Area(String code, String name) {
    public String getCode() {
      return code;
    }

    public String getName() {
      return name;
    }
  }

  private final Config config;
  private final ObjectMapper json = new ObjectMapper();
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

  private record Codes(long expires, Map<String, String> values) {}

  private final Map<String, Codes> areaCache = new java.util.concurrent.ConcurrentHashMap<>();

  public TourClient(Config config) {
    this.config = config;
  }

  public Result search(AttractionQuery q) {
    if (config.demo()) return demo(q);
    Map<String, String> params = new LinkedHashMap<>();
    params.put("lDongRegnCd", q.area());
    params.put("lDongSignguCd", q.district());
    params.put("contentTypeId", q.type());
    params.put("arrange", q.sort());
    params.put("pageNo", "" + q.page());
    params.put("numOfRows", "" + q.size());
    if (!q.keyword().isEmpty()) params.put("keyword", q.keyword());
    JsonNode body = call(q.keyword().isEmpty() ? "areaBasedList2" : "searchKeyword2", params);
    List<Attraction> list = new ArrayList<>();
    for (JsonNode node : items(body)) list.add(parse(node));
    return new Result(list, body.path("totalCount").asInt());
  }

  public Attraction detail(String id) {
    if (!id.matches("[0-9]{1,12}")) throw new IllegalArgumentException("여행지 번호가 잘못되었습니다.");
    if (config.demo())
      return samples().stream().filter(a -> a.contentId().equals(id)).findFirst().orElse(null);
    JsonNode body = call("detailCommon2", Map.of("contentId", id));
    List<JsonNode> nodes = items(body);
    return nodes.isEmpty() ? null : parse(nodes.get(0));
  }

  public List<Area> districts(String area) {
    if (area.isEmpty()) return List.of();
    if (!regions().containsKey(area)) throw new IllegalArgumentException("지역이 잘못되었습니다.");
    if (config.demo()) return List.of();
    return codes(area).entrySet().stream().map(e -> new Area(e.getKey(), e.getValue())).toList();
  }

  public Map<String, String> regions() {
    return config.demo() ? Collections.unmodifiableMap(REGIONS) : codes("");
  }

  private synchronized Map<String, String> codes(String area) {
    Codes cached = areaCache.get(area);
    if (cached != null && cached.expires() > System.currentTimeMillis()) return cached.values();
    Map<String, String> result = new LinkedHashMap<>();
    for (JsonNode n : items(call("ldongCode2", Map.of("lDongRegnCd", area, "numOfRows", "100"))))
      result.put(n.path("code").asText(), n.path("name").asText());
    Map<String, String> values = Collections.unmodifiableMap(result);
    areaCache.put(area, new Codes(System.currentTimeMillis() + 86_400_000L, values));
    return values;
  }

  private JsonNode call(String operation, Map<String, String> params) {
    StringBuilder query =
        new StringBuilder("serviceKey=")
            .append(Web.encode(config.require("tour.service-key")))
            .append("&MobileOS=ETC&MobileApp=EnjoyTrip&_type=json");
    params.forEach(
        (key, value) -> {
          if (!value.isBlank()) query.append('&').append(key).append('=').append(Web.encode(value));
        });
    try {
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(
                      URI.create(config.get("tour.base-url") + "/" + operation + "?" + query))
                  .timeout(Duration.ofSeconds(10))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      JsonNode payload = json.readTree(response.body());
      JsonNode root = payload.path("response");
      String code = root.path("header").path("resultCode").asText();
      if (code.isEmpty())
        code =
            payload
                .path("OpenAPI_ServiceResponse")
                .path("cmmMsgHeader")
                .path("returnReasonCode")
                .asText();
      if (code.isEmpty()) code = payload.path("resultCode").asText();
      if (response.statusCode() != 200 || (!code.equals("0000") && !code.equals("000"))) {
        String safeCode = code.matches("[A-Z0-9_]{1,40}") ? code : "UNKNOWN";
        throw new ApiFailure(
            "관광 정보 요청 실패 ("
                + operation
                + ", HTTP "
                + response.statusCode()
                + ", 코드 "
                + safeCode
                + "). 서비스 활용 등록과 제공 상태를 확인해 주세요.");
      }
      if (!code.equals("0000") && !code.equals("000")) throw new IllegalStateException();
      return root.path("body");
    } catch (ApiFailure e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("관광 정보 요청이 중단되었습니다.");
    } catch (Exception e) {
      throw new IllegalStateException("관광 정보를 불러오지 못했습니다. 인증키와 API 서비스 상태를 확인해 주세요.");
    }
  }

  public static class ApiFailure extends IllegalStateException {
    public ApiFailure(String message) {
      super(message);
    }
  }

  public static List<JsonNode> items(JsonNode body) {
    JsonNode items = body.path("items").path("item");
    if (items.isArray()) {
      List<JsonNode> result = new ArrayList<>();
      items.forEach(result::add);
      return result;
    }
    return items.isObject() ? List.of(items) : List.of();
  }

  private Attraction parse(JsonNode n) {
    String image = imageUrl(n.path("firstimage").asText());
    String overview =
        n.path("overview").asText().replaceAll("(?i)<br\s*/?>", "\n").replaceAll("<[^>]*>", "");
    double lat = n.path("mapy").asDouble(), lon = n.path("mapx").asDouble();
    if (!Double.isFinite(lat)
        || !Double.isFinite(lon)
        || Math.abs(lat) > 90
        || Math.abs(lon) > 180) {
      lat = 0;
      lon = 0;
    }
    return new Attraction(
        n.path("contentid").asText(),
        n.path("title").asText(),
        n.path("addr1").asText() + " " + n.path("addr2").asText(),
        n.path("lDongRegnCd").asText(n.path("areacode").asText()),
        n.path("contenttypeid").asText(),
        image,
        lat,
        lon,
        overview);
  }

  public static String imageUrl(String value) {
    if (value.startsWith("http://tong.visitkorea.or.kr/")) value = "https://" + value.substring(7);
    try {
      URI uri = URI.create(value);
      return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
          ? uri.toASCIIString()
          : "";
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  private Result demo(AttractionQuery q) {
    var stream =
        samples().stream()
            .filter(a -> q.area().isEmpty() || a.areaCode().equals(q.area()))
            .filter(a -> q.type().isEmpty() || a.contentTypeId().equals(q.type()))
            .filter(
                a ->
                    q.keyword().isEmpty()
                        || a.title().contains(q.keyword())
                        || a.address().contains(q.keyword()));
    List<Attraction> values = stream.sorted(Comparator.comparing(Attraction::title)).toList();
    int start = Math.min(values.size(), (q.page() - 1) * q.size());
    return new Result(
        values.subList(start, Math.min(values.size(), start + q.size())), values.size());
  }

  private List<Attraction> samples() {
    List<Attraction> values = new ArrayList<>();
    int id = 1000;
    for (String area : List.of("11", "26", "50"))
      for (String type : TYPES.keySet()) {
        String title = "[샘플] " + REGIONS.get(area) + " " + TYPES.get(type);
        values.add(
            new Attraction(
                "" + (id++),
                title,
                REGIONS.get(area) + " · 개발용 예시 주소",
                area,
                type,
                "",
                area.equals("11") ? 37.5796 : area.equals("26") ? 35.1587 : 33.4996,
                area.equals("11") ? 126.977 : area.equals("26") ? 129.1604 : 126.5312,
                "화면과 검색 기능 확인을 위한 개발용 샘플입니다. 실제 관광지의 운영 정보가 아닙니다."));
      }
    return values;
  }
}
