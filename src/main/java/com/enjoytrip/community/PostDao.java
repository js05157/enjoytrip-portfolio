package com.enjoytrip.community;
import com.enjoytrip.common.Database;
import java.sql.*;
import java.util.*;
public class PostDao {
 public record Post(long id,long memberId,String board,String title,String body,String author,String createdAt){}
 private final Database db;public PostDao(Database db){this.db=db;}
 private Post read(ResultSet r)throws SQLException{return new Post(r.getLong("id"),r.getLong("member_id"),r.getString("board"),r.getString("title"),r.getString("body"),r.getString("name"),r.getTimestamp("created_at").toLocalDateTime().toString().replace('T',' '));}
 public List<Post> list(String board,int page){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT p.*,m.name FROM posts p JOIN members m ON m.id=p.member_id WHERE p.board=? ORDER BY p.id DESC LIMIT 20 OFFSET ?")){s.setString(1,board);s.setInt(2,(page-1)*20);List<Post> rows=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())rows.add(read(r));}return rows;}catch(SQLException e){throw new IllegalStateException(e);}}
 public Post get(long id){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("SELECT p.*,m.name FROM posts p JOIN members m ON m.id=p.member_id WHERE p.id=?")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){return r.next()?read(r):null;}}catch(SQLException e){throw new IllegalStateException(e);}}
 public long create(long owner,String board,String title,String body){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("INSERT INTO posts(member_id,board,title,body) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setLong(1,owner);s.setString(2,board);s.setString(3,title);s.setString(4,body);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}}catch(SQLException e){throw new IllegalStateException(e);}}
 public boolean update(long id,long owner,boolean admin,String title,String body){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("UPDATE posts SET title=?,body=? WHERE id=? AND (member_id=? OR ?=TRUE)")){s.setString(1,title);s.setString(2,body);s.setLong(3,id);s.setLong(4,owner);s.setBoolean(5,admin);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
 public boolean delete(long id,long owner,boolean admin){try(Connection c=db.connection();PreparedStatement s=c.prepareStatement("DELETE FROM posts WHERE id=? AND (member_id=? OR ?=TRUE)")){s.setLong(1,id);s.setLong(2,owner);s.setBoolean(3,admin);return s.executeUpdate()==1;}catch(SQLException e){throw new IllegalStateException(e);}}
}
