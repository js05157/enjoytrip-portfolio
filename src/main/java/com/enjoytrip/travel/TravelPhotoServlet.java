package com.enjoytrip.travel;

import com.enjoytrip.common.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/travel-photos")
public class TravelPhotoServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    var service = App.of(getServletContext()).attractions;
    String area = Web.text(req, "area", 2);
    int page = Web.number(req, "page", 1, 1, 1000);
    req.setAttribute("regions", service.regions());
    req.setAttribute("area", area);
    req.setAttribute("page", page);
    var result = service.recommendedPhotos(area, page);
    req.setAttribute("result", result);
    req.setAttribute("hasNext", page * 12 < result.total());
    String id = Web.text(req, "contentId", 12);
    if (!id.isEmpty()) {
      var item = service.detail(id);
      if (item == null) {
        res.sendError(404);
        return;
      }
      req.setAttribute("item", item);
      try {
        req.setAttribute("photos", service.photos(id));
      } catch (com.enjoytrip.attraction.TourClient.ApiFailure e) {
        req.setAttribute("photoError", e.getMessage());
      }
    }
    Web.view(req, res, "travel/photos");
  }
}
