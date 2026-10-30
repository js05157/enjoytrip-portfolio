package com.enjoytrip.member;
import com.enjoytrip.common.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/members/*")
public class MemberServlet extends HttpServlet {
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  if(!"/me".equals(req.getPathInfo())){res.sendError(404);return;}
  if(Auth.require(req,res)==null)return;Web.view(req,res,"members/me");
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  Member current=Auth.require(req,res);if(current==null)return;MemberService service=App.of(getServletContext()).members;
  try{
   if("/update".equals(req.getPathInfo())){Member next=service.update(current,Web.text(req,"email",254),Web.text(req,"name",40),req.getParameter("password"),req.getParameter("newPassword"));Auth.login(req,next);Web.flash(req,"회원 정보를 수정했어요.");Web.redirect(req,res,"/members/me");}
   else if("/delete".equals(req.getPathInfo())){var images=new com.enjoytrip.place.HotplaceDao(App.of(getServletContext()).db).images(current.id());service.delete(current,req.getParameter("password"));var store=new com.enjoytrip.place.ImageStore();images.forEach(store::delete);req.getSession().invalidate();Web.flash(req,"회원 탈퇴가 완료됐어요.");Web.redirect(req,res,"/");}
   else res.sendError(404);
  }catch(IllegalArgumentException e){res.setStatus(400);req.setAttribute("error",e.getMessage());Web.view(req,res,"members/me");}
 }
}
