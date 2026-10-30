package com.enjoytrip.plan;

import com.enjoytrip.attraction.*;
import com.enjoytrip.common.*;
import com.enjoytrip.member.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@WebServlet("/plans/*")
public class PlanServlet extends HttpServlet {
  private PlanDao dao;

  @Override
  public void init() {
    dao = new PlanDao(App.of(getServletContext()).db);
  }

  public static BigDecimal money(String value) {
    try {
      BigDecimal n = new BigDecimal(value.isBlank() ? "0" : value).setScale(2);
      if (n.signum() < 0 || n.compareTo(new BigDecimal("1000000000")) > 0)
        throw new NumberFormatException();
      return n;
    } catch (ArithmeticException | NumberFormatException e) {
      throw new IllegalArgumentException("경비는 0~10억 사이의 금액으로 입력하세요.");
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {
    Member member = Auth.require(req, res);
    if (member == null) return;
    String path = req.getPathInfo();
    if (path == null || path.equals("/")) {
      req.setAttribute("plans", dao.list(member.id()));
      Web.view(req, res, "plans/list");
      return;
    }
    if (!path.equals("/detail")) {
      res.sendError(404);
      return;
    }
    long id = Web.number(req, "id", 0, 1, Integer.MAX_VALUE);
    var plan = dao.get(id, member.id());
    if (plan == null) {
      res.sendError(404);
      return;
    }
    var stops = dao.stops(id, member.id());
    req.setAttribute("plan", plan);
    req.setAttribute("stops", stops);
    req.setAttribute(
        "totalCost",
        stops.stream().map(PlanDao.Stop::cost).reduce(BigDecimal.ZERO, BigDecimal::add));
    req.setAttribute(
        "days", java.time.temporal.ChronoUnit.DAYS.between(plan.startDate(), plan.endDate()) + 1);
    Web.view(req, res, "plans/detail");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    Member member = Auth.require(req, res);
    if (member == null) return;
    String path = req.getPathInfo();
    if ("/create".equals(path)) {
      String title = Web.text(req, "title", 100);
      if (title.isEmpty()) throw new IllegalArgumentException("여행 이름을 입력하세요.");
      LocalDate start, end;
      try {
        start = LocalDate.parse(Web.text(req, "start", 10));
        end = LocalDate.parse(Web.text(req, "end", 10));
      } catch (Exception e) {
        throw new IllegalArgumentException("시작일과 종료일을 확인하세요.");
      }
      if (end.isBefore(start) || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 30)
        throw new IllegalArgumentException("여행 기간은 1~31일로 선택하세요.");
      long id =
          dao.create(
              member.id(),
              title,
              start,
              end,
              money(Web.text(req, "budget", 20)),
              Web.text(req, "description", 3000));
      Web.redirect(req, res, "/plans/detail?id=" + id);
      return;
    }
    long id = Web.number(req, "id", 0, 1, Integer.MAX_VALUE);
    if (dao.get(id, member.id()) == null) {
      res.sendError(404);
      return;
    }
    if ("/delete".equals(path)) {
      dao.delete(id, member.id());
      Web.redirect(req, res, "/plans");
      return;
    }
    if ("/add".equals(path)) {
      Attraction a = App.of(getServletContext()).attractions.detail(Web.text(req, "contentId", 12));
      if (a == null) throw new IllegalArgumentException("여행지를 찾을 수 없습니다.");
      dao.add(
          id,
          member.id(),
          Web.number(req, "day", 1, 1, 31),
          a,
          money(Web.text(req, "cost", 20)),
          Web.text(req, "notes", 1000));
    } else if ("/remove".equals(path))
      dao.remove(id, member.id(), Web.number(req, "stop", 0, 1, Integer.MAX_VALUE));
    else if ("/reorder".equals(path)) {
      String order = Web.text(req, "order", 1500);
      try {
        dao.reorder(
            id,
            member.id(),
            order.isEmpty()
                ? List.of()
                : Arrays.stream(order.split(",")).map(Long::valueOf).toList());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("정렬 값이 잘못되었습니다.");
      }
    } else {
      res.sendError(404);
      return;
    }
    Web.redirect(req, res, "/plans/detail?id=" + id);
  }
}
