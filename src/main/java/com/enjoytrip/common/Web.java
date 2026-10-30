package com.enjoytrip.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class Web {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Web() {}

  public static String token() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String text(HttpServletRequest req, String name, int max) {
    String value = req.getParameter(name);
    value = value == null ? "" : value.trim();
    if (value.length() > max) throw new IllegalArgumentException("입력값이 너무 깁니다: " + name);
    return value;
  }

  public static int number(HttpServletRequest req, String name, int fallback, int min, int max) {
    String value = req.getParameter(name);
    if (value == null || value.isBlank()) return fallback;
    try {
      int result = Integer.parseInt(value);
      if (result < min || result > max) throw new NumberFormatException();
      return result;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("잘못된 조회 조건입니다: " + name);
    }
  }

  public static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public static void view(HttpServletRequest req, HttpServletResponse res, String page)
      throws ServletException, IOException {
    req.getRequestDispatcher("/WEB-INF/views/" + page + ".jsp").forward(req, res);
  }

  public static void redirect(HttpServletRequest req, HttpServletResponse res, String path)
      throws IOException {
    res.sendRedirect(req.getContextPath() + path);
  }

  public static void flash(HttpServletRequest req, String message) {
    req.getSession().setAttribute("flash", message);
  }
}
