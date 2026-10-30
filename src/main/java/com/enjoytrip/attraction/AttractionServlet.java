package com.enjoytrip.attraction;

import com.enjoytrip.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/attractions/*")
public class AttractionServlet extends HttpServlet {
  private AttractionService service;

  @Override
  public void init() {
    service = App.of(getServletContext()).attractions;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    String path = req.getPathInfo();
    if ("/areas".equals(path)) {
      res.setContentType("application/json;charset=UTF-8");
      new ObjectMapper().writeValue(res.getWriter(), service.districts(Web.text(req, "area", 2)));
      return;
    }
    if ("/detail".equals(path)) {
      Attraction item = service.detail(Web.text(req, "id", 12));
      if (item == null) {
        res.sendError(404);
        return;
      }
      req.setAttribute("item", item);
      com.enjoytrip.member.Member member = com.enjoytrip.member.Auth.current(req);
      if (member != null)
        req.setAttribute(
            "plans",
            new com.enjoytrip.plan.PlanDao(App.of(getServletContext()).db).list(member.id()));
      Web.view(req, res, "attractions/detail");
      return;
    }
    if (path != null && !path.equals("/")) {
      res.sendError(404);
      return;
    }
    AttractionQuery query = AttractionQuery.of(req);
    req.setAttribute("query", query);
    req.setAttribute("result", service.search(query));
    req.setAttribute("regions", service.regions());
    req.setAttribute("types", TourClient.TYPES);
    req.setAttribute("districts", service.districts(query.area()));
    req.setAttribute("page", query.page());
    req.setAttribute(
        "hasNext",
        query.page() * query.size() < ((TourClient.Result) req.getAttribute("result")).total());
    Web.view(req, res, "attractions/list");
  }
}
