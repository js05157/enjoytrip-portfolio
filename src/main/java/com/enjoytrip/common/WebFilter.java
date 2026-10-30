package com.enjoytrip.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@jakarta.servlet.annotation.WebFilter("/*")
public class WebFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    req.setCharacterEncoding("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader(
        "Referrer-Policy",
        req.getRequestURI().contains("/auth/reset")
            ? "no-referrer"
            : "strict-origin-when-cross-origin");
    res.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; img-src 'self' https: data:; style-src 'self'; script-src 'self';"
            + " frame-src https://www.openstreetmap.org; base-uri 'self'; form-action 'self';"
            + " frame-ancestors 'none'");
    if (req.getRequestURI().startsWith(req.getContextPath() + "/assets/")) {
      chain.doFilter(req, res);
      return;
    }
    try {
      if (req.getContentLengthLong() > 6L * 1024 * 1024) {
        res.setStatus(413);
        req.setAttribute("error", "업로드 용량이 너무 큽니다. 5MB 이하의 사진을 선택하세요.");
        Web.view(req, res, "error");
        return;
      }
      com.enjoytrip.member.Auth.current(req);
      HttpSession session = req.getSession();
      if (session.getAttribute("csrf") == null) session.setAttribute("csrf", Web.token());
      res.setHeader("Cache-Control", "no-store");
      if ("POST".equals(req.getMethod())) {
        String actual = req.getParameter("csrf"), expected = (String) session.getAttribute("csrf");
        if (actual == null
            || !MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8))) {
          res.setStatus(403);
          req.setAttribute("error", "요청이 만료되었습니다. 화면을 새로고침한 뒤 다시 시도하세요.");
          Web.view(req, res, "error");
          return;
        }
      }
      chain.doFilter(req, res);
    } catch (IllegalArgumentException e) {
      if (res.isCommitted()) throw e;
      res.resetBuffer();
      res.setStatus(400);
      req.setAttribute("error", e.getMessage());
      Web.view(req, res, "error");
    } catch (Exception e) {
      req.getServletContext().log("요청 처리 실패: " + req.getRequestURI(), e);
      if (res.isCommitted()) throw new ServletException("요청 처리 실패");
      res.resetBuffer();
      res.setStatus(500);
      req.setAttribute("error", "요청을 처리하지 못했습니다. 잠시 후 다시 시도하세요.");
      Web.view(req, res, "error");
    }
  }
}
