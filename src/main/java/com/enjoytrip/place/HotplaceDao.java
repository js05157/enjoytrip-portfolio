package com.enjoytrip.place;
import com.enjoytrip.common.Database;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;
public class HotplaceDao {
 public record Place(long id,long memberId,String title,String address,String type,String visitedOn,String description,String photo,double latitude,double longitude,String author){}
 private final Database db;public HotplaceDao(Database db){this.db=db;}
 public List<Place> list(int page){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT h.*,m.name FROM hotplaces h JOIN members m ON m.id=h.member_id ORDER BY h.id DESC LIMIT 12 OFFSET ?")){s.setInt(1,(page-1)*12);List<Place> rows=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Place(r.getLong("id"),r.getLong("member_id"),r.getString("title"),r.getString("address"),r.getString("place_type"),r.getDate("visited_on").toString(),r.getString("description"),r.getString("photo"),r.getDouble("latitude"),r.getDouble("longitude"),r.getString("name")));}return rows;}catch(SQLException e){throw new IllegalStateException(e);}}
 public void create(long owner,String title,String address,String type,LocalDate date,String description,String photo,double lat,double lon){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("INSERT INTO hotplaces(member_id,title,address,place_type,visited_on,description,photo,latitude,longitude) VALUES(?,?,?,?,?,?,?,?,?)")){s.setLong(1,owner);s.setString(2,title);s.setString(3,address);s.setString(4,type);s.setDate(5,java.sql.Date.valueOf(date));s.setString(6,description);s.setString(7,photo);s.setDouble(8,lat);s.setDouble(9,lon);s.executeUpdate();}catch(SQLException e){throw new IllegalStateException(e);}}
 public List<String> images(long owner){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT photo FROM hotplaces WHERE member_id=?")){s.setLong(1,owner);List<String> names=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())names.add(r.getString(1));}return names;}catch(SQLException e){throw new IllegalStateException(e);}}
}
