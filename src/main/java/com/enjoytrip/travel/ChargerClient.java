package com.enjoytrip.travel;

import com.enjoytrip.attraction.TourClient;
import com.enjoytrip.common.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ChargerClient {
  private final Config config;
  private final JsonApi api;

  public ChargerClient(Config config, JsonApi api) {
    this.config = config;
    this.api = api;
  }

  public boolean configured() {
    return !config.get("ev.service-key").isBlank();
  }

  public record Query(String region, int page) {
    public Query {
      if (!region.matches("[0-9]{2}") || page < 1 || page > 1000)
        throw new IllegalArgumentException("충전소 지역과 페이지를 확인하세요.");
    }
  }

  public record Charger(
      String id,
      String name,
      String chargerId,
      String type,
      String address,
      double latitude,
      double longitude,
      String status,
      String updated,
      String hours,
      String operator,
      String phone,
      String capacity,
      String parking,
      String note) {
    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getChargerId() {
      return chargerId;
    }

    public String getType() {
      return type;
    }

    public String getAddress() {
      return address;
    }

    public double getLatitude() {
      return latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    public boolean getHasCoordinates() {
      return Double.isFinite(latitude)
          && Double.isFinite(longitude)
          && latitude >= 32
          && latitude <= 40
          && longitude >= 124
          && longitude <= 132;
    }

    public boolean hasCoordinates() {
      return getHasCoordinates();
    }

    public String getStatus() {
      return status;
    }

    public String getUpdated() {
      return updated;
    }

    public String getHours() {
      return hours;
    }

    public String getOperator() {
      return operator;
    }

    public String getPhone() {
      return phone;
    }

    public String getCapacity() {
      return capacity;
    }

    public String getParking() {
      return parking;
    }

    public String getNote() {
      return note;
    }
  }

  public record Result(List<Charger> items, int total, String retrievedAt) {
    public List<Charger> getItems() {
      return items;
    }

    public int getTotal() {
      return total;
    }

    public String getRetrievedAt() {
      return retrievedAt;
    }
  }

  public Result search(Query q) {
    if (!configured()) throw new JsonApi.Failure("충전소 정보 제공을 준비하고 있습니다. 잠시 후 다시 확인해 주세요.");
    URI uri =
        URI.create(
            config.get("ev.base-url")
                + "/getChargerInfo?ServiceKey="
                + Web.encode(config.require("ev.service-key"))
                + "&dataType=JSON&numOfRows=24&pageNo="
                + q.page()
                + "&zcode="
                + q.region());
    JsonNode root = api.get("충전소", uri, 60, ChargerClient::successful);
    JsonNode body = root.has("response") ? root.path("response").path("body") : root;
    List<Charger> chargers = new ArrayList<>();
    for (JsonNode n : TourClient.items(body)) {
      chargers.add(
          new Charger(
              n.path("statId").asText(),
              n.path("statNm").asText(),
              n.path("chgerId").asText(),
              type(n.path("chgerType").asText()),
              n.path("addr").asText(),
              n.path("lat").asDouble(),
              n.path("lng").asDouble(),
              status(n.path("stat").asText()),
              updated(n.path("statUpdDt").asText()),
              n.path("useTime").asText("정보 없음"),
              n.path("busiNm").asText(),
              n.path("busiCall").asText(),
              n.path("output").asText(n.path("powerType").asText("정보 없음")),
              switch (n.path("parkingFree").asText()) {
                case "Y" -> "무료";
                case "N" -> "유료";
                default -> "현장 확인";
              },
              n.path("note").asText()));
    }
    return new Result(
        List.copyOf(chargers),
        body.path("totalCount").asInt(),
        LocalDateTime.now(WeatherClient.SEOUL).withNano(0).toString().replace('T', ' '));
  }

  private static boolean successful(JsonNode root) {
    JsonNode h = root.has("response") ? root.path("response").path("header") : root;
    String code = h.path("resultCode").asText();
    return code.equals("00")
        || code.equals("0000")
        || (code.isEmpty()
            && h.path("resultMsg").asText().equals("NORMAL SERVICE.")
            && root.has("items"));
  }

  private static String updated(String value) {
    try {
      return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
          .toString()
          .replace('T', ' ');
    } catch (Exception e) {
      return "갱신 시각 정보 없음";
    }
  }

  private static String status(String code) {
    return switch (code) {
      case "1" -> "통신 이상";
      case "2" -> "충전 대기";
      case "3" -> "충전 중";
      case "4" -> "운영 중지";
      case "5" -> "점검 중";
      case "9" -> "상태 미확인";
      default -> "상태 정보 없음";
    };
  }

  private static String type(String code) {
    return switch (code) {
      case "01" -> "DC 차데모";
      case "02" -> "AC 완속";
      case "03" -> "DC 차데모 + AC 3상";
      case "04" -> "DC 콤보";
      case "05" -> "DC 차데모 + DC 콤보";
      case "06" -> "DC 차데모 + AC 3상 + DC 콤보";
      case "07" -> "AC 3상";
      case "08" -> "DC 콤보(완속)";
      default -> "기타 · 현장 확인";
    };
  }
}
