package com.enjoytrip.common;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class App implements ServletContextListener {
  public Config config;
  public Database db;
  public com.enjoytrip.cache.RedisCache cache;
  public com.enjoytrip.member.ResetService resets;
  public com.enjoytrip.member.MemberService members;
  public com.enjoytrip.attraction.AttractionService attractions;

  public static App of(ServletContext context) {
    return (App) context.getAttribute("app");
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    config = new Config();
    db = new Database(config);
    event.getServletContext().getSessionCookieConfig().setAttribute("SameSite", "Lax");
    event
        .getServletContext()
        .getSessionCookieConfig()
        .setSecure(config.get("app.base-url").startsWith("https://"));
    resets = new com.enjoytrip.member.ResetService(db, config);
    members = new com.enjoytrip.member.MemberService(new com.enjoytrip.member.MemberDao(db));
    cache = new com.enjoytrip.cache.RedisCache(config);
    attractions =
        new com.enjoytrip.cache.CachedAttractionService(
            new com.enjoytrip.attraction.TourClient(config),
            cache,
            config.number("redis.ttl-seconds"),
            config.demo() ? "demo" : "live");
    event.getServletContext().setAttribute("app", this);
    event.getServletContext().setAttribute("demo", config.demo());
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (cache != null) cache.close();
    if (db != null) db.close();
  }
}
