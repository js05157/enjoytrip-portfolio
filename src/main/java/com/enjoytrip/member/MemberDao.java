package com.enjoytrip.member;

import com.enjoytrip.common.Database;
import java.sql.*;

public class MemberDao {
  private final Database db;

  public MemberDao(Database db) {
    this.db = db;
  }

  private Member read(ResultSet r) throws SQLException {
    return new Member(
        r.getLong("id"),
        r.getString("email"),
        r.getString("name"),
        r.getString("password_hash"),
        r.getInt("session_version"));
  }

  public Member byId(long id) {
    return find("id", id);
  }

  public Member byEmail(String email) {
    return find("email", email);
  }

  private Member find(String field, Object value) {
    try (Connection c = db.connection();
        PreparedStatement s = c.prepareStatement("SELECT * FROM members WHERE " + field + "=?")) {
      s.setObject(1, value);
      try (ResultSet r = s.executeQuery()) {
        return r.next() ? read(r) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("회원 조회 실패", e);
    }
  }

  public Member insert(String email, String name, String hash) {
    try (Connection c = db.connection();
        PreparedStatement s =
            c.prepareStatement(
                "INSERT INTO members(email,name,password_hash) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
      s.setString(1, email);
      s.setString(2, name);
      s.setString(3, hash);
      s.executeUpdate();
      try (ResultSet r = s.getGeneratedKeys()) {
        r.next();
        return new Member(r.getLong(1), email, name, hash, 0);
      }
    } catch (SQLException e) {
      if (e.getSQLState() != null && e.getSQLState().startsWith("23"))
        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
      throw new IllegalStateException("회원 가입 실패", e);
    }
  }

  public Member update(long id, String email, String name, String hash) {
    try (Connection c = db.connection();
        PreparedStatement s =
            c.prepareStatement(
                "UPDATE members SET"
                    + " email=?,name=?,password_hash=?,session_version=session_version+1 WHERE"
                    + " id=?")) {
      s.setString(1, email);
      s.setString(2, name);
      s.setString(3, hash);
      s.setLong(4, id);
      if (s.executeUpdate() != 1) throw new IllegalArgumentException("회원을 찾을 수 없습니다.");
    } catch (SQLException e) {
      if (e.getSQLState() != null && e.getSQLState().startsWith("23"))
        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
      throw new IllegalStateException("회원 수정 실패", e);
    }
    return byId(id);
  }

  public void delete(long id) {
    try (Connection c = db.connection();
        PreparedStatement s = c.prepareStatement("DELETE FROM members WHERE id=?")) {
      s.setLong(1, id);
      s.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("회원 탈퇴 실패", e);
    }
  }
}
