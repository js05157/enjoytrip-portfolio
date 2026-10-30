package com.enjoytrip.place;
import com.enjoytrip.common.*;
import com.enjoytrip.member.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
@WebServlet(urlPatterns={"/places/*","/photos/*"})
@MultipartConfig(maxFileSize=5*1024*1024,maxRequestSize=6*1024*1024,fileSizeThreshold=1024*1024)
public class PlaceServlet extends HttpServlet {
 private HotplaceDao dao;private ImageStore store;
 @Override public void init(){dao=new HotplaceDao(App.of(getServletContext()).db);store=new ImageStore();}
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  if(req.getServletPath().equals("/photos")){String name=req.getPathInfo()==null?"":req.getPathInfo().substring(1);Path path;try{path=store.file(name);}catch(IllegalArgumentException e){res.sendError(404);return;}if(!Files.isRegularFile(path)){res.sendError(404);return;}res.setContentType("image/jpeg");res.setHeader("Cache-Control","private,max-age=3600");Files.copy(path,res.getOutputStream());return;}
  if(req.getPathInfo()!=null&&!req.getPathInfo().equals("/")){res.sendError(404);return;}
  int page=Web.number(req,"page",1,1,1000);var rows=dao.list(page);req.setAttribute("places",rows);req.setAttribute("page",page);req.setAttribute("hasNext",rows.size()==12);Web.view(req,res,"places/list");
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
  if(!req.getServletPath().equals("/places")||!"/create".equals(req.getPathInfo())){res.sendError(404);return;}
  Member member=Auth.require(req,res);if(member==null)return;
  String title=Web.text(req,"title",100),address=Web.text(req,"address",300),type=Web.text(req,"type",40),description=Web.text(req,"description",3000);
  if(title.isEmpty()||address.isEmpty()||type.isEmpty())throw new IllegalArgumentException("장소 이름, 주소, 유형을 입력하세요.");
  LocalDate date;double lat,lon;
  try{date=LocalDate.parse(Web.text(req,"visitedOn",10));lat=Double.parseDouble(Web.text(req,"latitude",20));lon=Double.parseDouble(Web.text(req,"longitude",20));if(date.isAfter(LocalDate.now())||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("방문 날짜와 위치 좌표를 확인하세요.");}
  Part photo=req.getPart("photo");if(photo==null||photo.getSize()==0)throw new IllegalArgumentException("여행 사진을 선택하세요.");
  String saved;try(InputStream input=photo.getInputStream()){saved=store.save(input);}try{dao.create(member.id(),title,address,type,date,description,saved,lat,lon);}catch(RuntimeException e){store.delete(saved);throw e;}
  Web.flash(req,"여행 사진을 등록했어요.");Web.redirect(req,res,"/places");
 }
}
