package com.enjoytrip.common;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
@WebListener
public class App implements ServletContextListener {
    public Config config;
    public Database db;
    public static App of(ServletContext context) { return (App) context.getAttribute("app"); }
    @Override public void contextInitialized(ServletContextEvent event) {
        config = new Config(); db = new Database(config);
        event.getServletContext().setAttribute("app", this);
        event.getServletContext().setAttribute("demo", config.demo());
    }
    @Override public void contextDestroyed(ServletContextEvent event) { if (db != null) db.close(); }
}
