package com.enjoytrip.member;
import com.enjoytrip.common.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
 private MemberService service;
 @Override public void init(){service=App.of(getServletContext()).members;}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  String path=req.getPathInfo();if("/login".equals(path)||"/register".equals(path)){Web.view(req,res,"auth"+path);return;}res.sendError(404);
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  String path=req.getPathInfo();
  if("/logout".equals(path)){req.getSession().invalidate();Web.redirect(req,res,"/");return;}
  if(!"/login".equals(path)&&!"/register".equals(path)){res.sendError(404);return;}
  try{
   String password=req.getParameter("password");
   Member member="/login".equals(path)?service.login(Web.text(req,"email",254),password):service.register(Web.text(req,"email",254),Web.text(req,"name",40),password);
   Auth.login(req,member);Web.flash(req,"/login".equals(path)?"다시 만나 반가워요!":"회원가입이 완료됐어요.");Web.redirect(req,res,"/attractions");
  }catch(IllegalArgumentException e){res.setStatus(400);req.setAttribute("error",e.getMessage());Web.view(req,res,"auth"+path);}
 }
}
