package com.enjoytrip.common;
import com.enjoytrip.cache.CachedAttractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;
@WebServlet("/health")
public class HealthServlet extends HttpServlet {
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{
  App app=App.of(getServletContext());boolean database;
  try(Connection c=app.db.connection()){database=c.isValid(2);}catch(Exception e){database=false;}
  res.setStatus(database?200:503);res.setContentType("application/json;charset=UTF-8");
  Map<String,Object> body=new LinkedHashMap<>();body.put("status",database?"UP":"DOWN");body.put("database",database?"UP":"DOWN");body.put("redis",!app.config.enabled("redis.enabled")?"DISABLED":app.cache.available()?"UP":"UNAVAILABLE");body.put("cache",((CachedAttractionService)app.attractions).metrics());body.put("demo",app.config.demo());
  new ObjectMapper().writeValue(res.getWriter(),body);
 }
}
