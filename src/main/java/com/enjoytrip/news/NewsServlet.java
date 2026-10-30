package com.enjoytrip.news;
import com.enjoytrip.common.*;
import com.enjoytrip.member.*;
import com.enjoytrip.community.CommunityServlet;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/news/*")
public class NewsServlet extends HttpServlet {
 private NewsService service;
 @Override public void init(){service=new NewsService(App.of(getServletContext()).db);}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  if(req.getPathInfo()!=null&&!req.getPathInfo().equals("/")){res.sendError(404);return;}
  int page=Web.number(req,"page",1,1,1000);var articles=service.list(page);req.setAttribute("articles",articles);req.setAttribute("page",page);req.setAttribute("hasNext",articles.size()==20);req.setAttribute("admin",CommunityServlet.admin(App.of(getServletContext()),Auth.current(req)));Web.view(req,res,"news/list");
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException{
  if(!"/collect".equals(req.getPathInfo())){res.sendError(404);return;}Member m=Auth.require(req,res);if(m==null)return;if(!CommunityServlet.admin(App.of(getServletContext()),m)){res.sendError(403);return;}
  int count=service.collect();Web.flash(req,"새 뉴스 "+count+"건을 저장했어요.");Web.redirect(req,res,"/news");
 }
}
