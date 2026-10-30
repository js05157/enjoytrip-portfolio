package com.enjoytrip.member;

import com.enjoytrip.common.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ResetService {
  private final Database db;
  private final Config config;
  private final MemberDao members;

  public ResetService(Database db, Config config) {
    this.db = db;
    this.config = config;
    members = new MemberDao(db);
  }

  public String request(String email) {
    if (!available()) throw new IllegalStateException("재설정 메일 서비스를 준비하고 있습니다.");
    Member member = members.byEmail(MemberService.email(email));
    if (member == null) return null;
    if (!config.demo()) config.require("smtp.host");
    String token = issue(member.id());
    String base = config.require("app.base-url");
    URI uri = URI.create(base);
    if (!Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) throw new IllegalStateException("app.base-url 설정을 확인하세요.");
    String link = base.replaceAll("/$", "") + "/auth/reset?token=" + token;
    if (config.demo()) return link;
    send(member.email(), link);
    return null;
  }

  public boolean available() {
    return config.demo()
        || (!config.get("smtp.host").isBlank()
            && !config.get("smtp.from").isBlank()
            && (config.get("smtp.user").isBlank() || !config.get("smtp.password").isBlank()));
  }

  String issue(long memberId) {
    String token = Web.token();
    try (Connection c = db.connection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement lock =
            c.prepareStatement("SELECT id FROM members WHERE id=? FOR UPDATE")) {
          lock.setLong(1, memberId);
          try (ResultSet rows = lock.executeQuery()) {
            if (!rows.next()) throw new IllegalArgumentException("회원이 없습니다.");
          }
        }
        try (PreparedStatement s =
            c.prepareStatement("DELETE FROM password_resets WHERE member_id=? OR expires_at<?")) {
          s.setLong(1, memberId);
          s.setTimestamp(2, Timestamp.from(Instant.now()));
          s.executeUpdate();
        }
        try (PreparedStatement s =
            c.prepareStatement(
                "INSERT INTO password_resets(token_hash,member_id,expires_at) VALUES(?,?,?)")) {
          s.setString(1, digest(token));
          s.setLong(2, memberId);
          s.setTimestamp(3, Timestamp.from(Instant.now().plusSeconds(900)));
          s.executeUpdate();
        }
        c.commit();
        return token;
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("재설정 요청 실패", e);
    }
  }

  public void reset(String token, String password) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
      throw new IllegalArgumentException("재설정 링크가 올바르지 않습니다.");
    String hash = Passwords.hash(password);
    try (Connection c = db.connection()) {
      c.setAutoCommit(false);
      try {
        long id;
        try (PreparedStatement s =
            c.prepareStatement(
                "SELECT member_id FROM password_resets WHERE token_hash=? AND expires_at>? FOR"
                    + " UPDATE")) {
          s.setString(1, digest(token));
          s.setTimestamp(2, Timestamp.from(Instant.now()));
          try (ResultSet r = s.executeQuery()) {
            if (!r.next()) throw new IllegalArgumentException("만료되었거나 이미 사용한 링크입니다. 다시 요청해 주세요.");
            id = r.getLong(1);
          }
        }
        try (PreparedStatement s =
            c.prepareStatement(
                "UPDATE members SET password_hash=?,session_version=session_version+1 WHERE"
                    + " id=?")) {
          s.setString(1, hash);
          s.setLong(2, id);
          s.executeUpdate();
        }
        try (PreparedStatement s =
            c.prepareStatement("DELETE FROM password_resets WHERE member_id=?")) {
          s.setLong(1, id);
          s.executeUpdate();
        }
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("비밀번호 변경 실패", e);
    }
  }

  private static String digest(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void send(String email, String link) {
    Properties props = new Properties();
    props.setProperty("mail.smtp.host", config.require("smtp.host"));
    props.setProperty("mail.smtp.port", config.get("smtp.port"));
    props.setProperty("mail.smtp.connectiontimeout", "5000");
    props.setProperty("mail.smtp.timeout", "5000");
    props.setProperty("mail.smtp.writetimeout", "5000");
    props.setProperty("mail.smtp.starttls.enable", config.get("smtp.starttls"));
    props.setProperty("mail.smtp.starttls.required", config.get("smtp.starttls"));
    props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
    boolean auth = !config.get("smtp.user").isBlank();
    props.setProperty("mail.smtp.auth", "" + auth);
    Session session =
        Session.getInstance(
            props,
            auth
                ? new Authenticator() {
                  protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        config.get("smtp.user"), config.get("smtp.password"));
                  }
                }
                : null);
    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(config.require("smtp.from")));
      message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
      message.setSubject("[EnjoyTrip] 비밀번호 재설정", "UTF-8");
      message.setText(
          "15분 안에 다음 링크에서 비밀번호를 변경해 주세요.\n본인이 요청하지 않았다면 이 메일을 무시하세요.\n\n" + link, "UTF-8");
      Transport.send(message);
    } catch (MessagingException e) {
      throw new IllegalStateException("재설정 안내 메일을 전송하지 못했습니다. 잠시 후 다시 요청해 주세요.");
    }
  }
}
