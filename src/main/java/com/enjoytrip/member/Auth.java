package com.enjoytrip.member;
import com.enjoytrip.common.*;
import jakarta.servlet.http.*;
import java.io.IOException;
public final class Auth {
 private Auth(){}
 public static Member current(HttpServletRequest req){
  HttpSession s=req.getSession(false);if(s==null||!(s.getAttribute("member") instanceof Member saved))return null;
  Member current=App.of(req.getServletContext()).members.byId(saved.id());
  if(current==null||current.sessionVersion()!=saved.sessionVersion()){s.invalidate();return null;}
  s.setAttribute("member",current);return current;
 }
 public static Member require(HttpServletRequest req,HttpServletResponse res)throws IOException{
  Member m=current(req);if(m==null){Web.flash(req,"로그인 후 이용할 수 있어요.");Web.redirect(req,res,"/auth/login");}return m;
 }
 public static void login(HttpServletRequest req,Member member){
  req.getSession();req.changeSessionId();req.getSession().setAttribute("member",member);req.getSession().setAttribute("csrf",Web.token());
 }
}
