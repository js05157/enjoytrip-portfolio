package com.enjoytrip.community;
import com.enjoytrip.common.*;
import com.enjoytrip.member.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
@WebServlet("/community/*")
public class CommunityServlet extends HttpServlet {
 private PostDao dao;
 @Override public void init(){dao=new PostDao(App.of(getServletContext()).db);}
 public static boolean admin(App app,Member member){return member!=null&&Arrays.stream(app.config.get("app.admin-emails").split(",")).map(String::trim).anyMatch(email->!email.isBlank()&&email.equalsIgnoreCase(member.email()));}
 private String board(HttpServletRequest req){String value=Web.text(req,"board",10);return value.equals("notice")?"notice":"share";}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  Member member=Auth.current(req);boolean admin=admin(App.of(getServletContext()),member);req.setAttribute("admin",admin);String path=req.getPathInfo();String board=board(req);req.setAttribute("board",board);
  if(path==null||path.equals("/")){int page=Web.number(req,"page",1,1,1000);var rows=dao.list(board,page);req.setAttribute("posts",rows);req.setAttribute("page",page);req.setAttribute("hasNext",rows.size()==20);Web.view(req,res,"community/list");return;}
  if(path.equals("/new")){if(Auth.require(req,res)==null)return;if(board.equals("notice")&&!admin){res.sendError(403);return;}Web.view(req,res,"community/form");return;}
  long id=Web.number(req,"id",0,1,Integer.MAX_VALUE);var post=dao.get(id);if(post==null){res.sendError(404);return;}boolean editable=member!=null&&(admin||member.id()==post.memberId())&&(!post.board().equals("notice")||admin);req.setAttribute("post",post);req.setAttribute("editable",editable);req.setAttribute("board",post.board());
  if(path.equals("/detail"))Web.view(req,res,"community/detail");else if(path.equals("/edit")){if(!editable){res.sendError(403);return;}Web.view(req,res,"community/form");}else res.sendError(404);
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException{
  Member member=Auth.require(req,res);if(member==null)return;boolean admin=admin(App.of(getServletContext()),member);String path=req.getPathInfo();
  if("/create".equals(path)){String board=board(req);if(board.equals("notice")&&!admin){res.sendError(403);return;}String title=Web.text(req,"title",200),body=Web.text(req,"body",10000);if(title.isEmpty()||body.isEmpty())throw new IllegalArgumentException("제목과 내용을 입력하세요.");long id=dao.create(member.id(),board,title,body);Web.redirect(req,res,"/community/detail?id="+id);return;}
  long id=Web.number(req,"id",0,1,Integer.MAX_VALUE);var post=dao.get(id);if(post==null){res.sendError(404);return;}if((post.memberId()!=member.id()&&!admin)||(post.board().equals("notice")&&!admin)){res.sendError(403);return;}
  if("/update".equals(path)){String title=Web.text(req,"title",200),body=Web.text(req,"body",10000);if(title.isEmpty()||body.isEmpty())throw new IllegalArgumentException("제목과 내용을 입력하세요.");dao.update(id,member.id(),admin,title,body);Web.redirect(req,res,"/community/detail?id="+id);}
  else if("/delete".equals(path)){dao.delete(id,member.id(),admin);Web.redirect(req,res,"/community?board="+post.board());}else res.sendError(404);
 }
}
