package com.enjoytrip.common;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
@WebListener
public class App implements ServletContextListener {
    public Config config;
    public Database db;
    public com.enjoytrip.member.ResetService resets;
    public com.enjoytrip.member.MemberService members;
    public com.enjoytrip.attraction.AttractionService attractions;
    public static App of(ServletContext context) { return (App) context.getAttribute("app"); }
    @Override public void contextInitialized(ServletContextEvent event) {
        config = new Config(); db = new Database(config);
        resets = new com.enjoytrip.member.ResetService(db,config);
        members = new com.enjoytrip.member.MemberService(new com.enjoytrip.member.MemberDao(db));
        attractions = new com.enjoytrip.attraction.AttractionService(new com.enjoytrip.attraction.TourClient(config));
        event.getServletContext().setAttribute("app", this);
        event.getServletContext().setAttribute("demo", config.demo());
    }
    @Override public void contextDestroyed(ServletContextEvent event) { if (db != null) db.close(); }
}
