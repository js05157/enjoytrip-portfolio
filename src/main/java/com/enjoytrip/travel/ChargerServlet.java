package com.enjoytrip.travel;

import com.enjoytrip.attraction.TourClient;
import com.enjoytrip.common.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/chargers")
public class ChargerServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    App app = App.of(getServletContext());
    String region = req.getParameter("region") == null ? "11" : Web.text(req, "region", 2);
    int page = Web.number(req, "page", 1, 1, 1000);
    var query = new ChargerClient.Query(region, page);
    req.setAttribute("region", region);
    req.setAttribute("regions", TourClient.REGIONS);
    req.setAttribute("page", page);
    var client = new ChargerClient(app.config, app.external);
    req.setAttribute("configured", client.configured());
    if (client.configured()) {
      try {
        var result = client.search(query);
        req.setAttribute("result", result);
        req.setAttribute("hasNext", page * 24 < result.total());
      } catch (JsonApi.Failure e) {
        res.setStatus(502);
        req.setAttribute("error", e.getMessage());
      }
    }
    Web.view(req, res, "travel/chargers");
  }
}
