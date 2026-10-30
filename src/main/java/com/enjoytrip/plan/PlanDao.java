package com.enjoytrip.plan;
import com.enjoytrip.common.Database;
import com.enjoytrip.attraction.Attraction;
import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
public class PlanDao {
 public record Plan(long id,String title,LocalDate startDate,LocalDate endDate,BigDecimal budget,String description){}
 public record Stop(long id,int dayNumber,String title,String address,double latitude,double longitude,BigDecimal cost,String notes){}
 private final Database db;public PlanDao(Database db){this.db=db;}
 private Plan read(ResultSet r)throws SQLException{return new Plan(r.getLong("id"),r.getString("title"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getBigDecimal("budget"),r.getString("description"));}
 public List<Plan> list(long owner){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT * FROM plans WHERE member_id=? ORDER BY id DESC")){s.setLong(1,owner);List<Plan> rows=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())rows.add(read(r));}return rows;}catch(SQLException e){throw new IllegalStateException(e);}}
 public Plan get(long id,long owner){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT * FROM plans WHERE id=? AND member_id=?")){s.setLong(1,id);s.setLong(2,owner);try(ResultSet r=s.executeQuery()){return r.next()?read(r):null;}}catch(SQLException e){throw new IllegalStateException(e);}}
 public long create(long owner,String title,LocalDate start,LocalDate end,BigDecimal budget,String description){
  try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("INSERT INTO plans(member_id,title,start_date,end_date,budget,description) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setLong(1,owner);s.setString(2,title);s.setDate(3,java.sql.Date.valueOf(start));s.setDate(4,java.sql.Date.valueOf(end));s.setBigDecimal(5,budget);s.setString(6,description);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}}catch(SQLException e){throw new IllegalStateException(e);}
 }
 public void delete(long id,long owner){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("DELETE FROM plans WHERE id=? AND member_id=?")){s.setLong(1,id);s.setLong(2,owner);s.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 public List<Stop> stops(long id,long owner){if(get(id,owner)==null)throw new IllegalArgumentException("여행 일정을 찾을 수 없습니다.");
  try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT * FROM plan_stops WHERE plan_id=? ORDER BY day_number,position,id")){s.setLong(1,id);List<Stop> rows=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Stop(r.getLong("id"),r.getInt("day_number"),r.getString("title"),r.getString("address"),r.getDouble("latitude"),r.getDouble("longitude"),r.getBigDecimal("cost"),r.getString("notes")));}return rows;}catch(SQLException e){throw new IllegalStateException(e);}
 }
 public void add(long id,long owner,int day,Attraction item,BigDecimal cost,String notes){
  Plan plan=get(id,owner);if(plan==null)throw new IllegalArgumentException("여행 일정을 찾을 수 없습니다.");
  if(day<1||day>java.time.temporal.ChronoUnit.DAYS.between(plan.startDate(),plan.endDate())+1)throw new IllegalArgumentException("여행 날짜 범위 안의 일차를 선택하세요.");
  List<Stop> existing=stops(id,owner);if(existing.size()>=50)throw new IllegalArgumentException("한 여행에 50곳까지 담을 수 있습니다.");
  try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("INSERT INTO plan_stops(plan_id,position,day_number,content_id,title,address,latitude,longitude,cost,notes) VALUES(?,?,?,?,?,?,?,?,?,?)")){s.setLong(1,id);s.setInt(2,existing.size());s.setInt(3,day);s.setString(4,item.contentId());s.setString(5,item.title());s.setString(6,item.address());s.setDouble(7,item.latitude());s.setDouble(8,item.longitude());s.setBigDecimal(9,cost);s.setString(10,notes);s.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}
 }
 public void remove(long plan,long owner,long stop){if(get(plan,owner)==null)throw new IllegalArgumentException("여행 일정을 찾을 수 없습니다.");try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("DELETE FROM plan_stops WHERE id=? AND plan_id=?")){s.setLong(1,stop);s.setLong(2,plan);s.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 public void reorder(long id,long owner,List<Long> order){
  if(get(id,owner)==null)throw new IllegalArgumentException("여행 일정을 찾을 수 없습니다.");
  try(Connection c=db.connection()){
   c.setAutoCommit(false);
   try{
    Set<Long> actual=new HashSet<>();try(PreparedStatement s=c.prepareStatement("SELECT id FROM plan_stops WHERE plan_id=? FOR UPDATE")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){while(r.next())actual.add(r.getLong(1));}}
    if(order.size()!=actual.size()||new HashSet<>(order).size()!=order.size()||!actual.equals(new HashSet<>(order)))throw new IllegalArgumentException("여행지 목록이 바뀌었습니다. 새로고침 후 다시 정렬해 주세요.");
    try(PreparedStatement s=c.prepareStatement("UPDATE plan_stops SET position=? WHERE id=? AND plan_id=?")){for(int i=0;i<order.size();i++){s.setInt(1,i);s.setLong(2,order.get(i));s.setLong(3,id);s.addBatch();}s.executeBatch();}c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }catch(SQLException e){throw new IllegalStateException(e);}
 }
}
