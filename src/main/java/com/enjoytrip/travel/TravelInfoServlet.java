package com.enjoytrip.travel;

import com.enjoytrip.common.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;

@WebServlet("/travel-info")
public class TravelInfoServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    App app = App.of(getServletContext());
    double latitude = 37.5665, longitude = 126.978;
    String name = "서울";
    if (req.getParameter("contentId") != null) {
      var item = app.attractions.detail(Web.text(req, "contentId", 12));
      if (item == null) {
        res.sendError(404);
        return;
      }
      if (!item.hasCoordinates()) throw new IllegalArgumentException("위치 정보가 없는 여행지입니다.");
      latitude = item.latitude();
      longitude = item.longitude();
      name = item.title();
    } else if (req.getParameter("latitude") != null) {
      try {
        latitude = Double.parseDouble(Web.text(req, "latitude", 24));
        longitude = Double.parseDouble(Web.text(req, "longitude", 24));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("위도와 경도를 숫자로 입력하세요.");
      }
      name = Web.text(req, "name", 100);
    }
    LocalDate start = LocalDate.now(WeatherClient.SEOUL), end = start.plusDays(6);
    try {
      if (req.getParameter("start") != null) start = LocalDate.parse(Web.text(req, "start", 10));
      if (req.getParameter("end") != null) end = LocalDate.parse(Web.text(req, "end", 10));
    } catch (Exception e) {
      throw new IllegalArgumentException("조회 날짜를 확인하세요.");
    }
    var query = new WeatherClient.Query(latitude, longitude, start, end);
    req.setAttribute("name", name);
    req.setAttribute("latitude", latitude);
    req.setAttribute("longitude", longitude);
    req.setAttribute("start", start);
    req.setAttribute("end", end);
    req.setAttribute("minDate", LocalDate.now(WeatherClient.SEOUL));
    req.setAttribute("maxDate", LocalDate.now(WeatherClient.SEOUL).plusDays(15));
    try {
      req.setAttribute("forecast", app.weather.forecast(query));
    } catch (JsonApi.Failure e) {
      res.setStatus(502);
      req.setAttribute("error", e.getMessage());
    }
    Web.view(req, res, "travel/weather");
  }
}
