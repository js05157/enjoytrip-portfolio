package com.enjoytrip.member;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.common.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ResetServiceTest {
  @TempDir Path temp;

  @Test
  void tokensAreSingleUseSupersededAndInvalidateSessions() throws Exception {
    Path file = temp.resolve("test.properties");
    Files.writeString(
        file, "db.url=jdbc:h2:mem:resets;MODE=MySQL;DATABASE_TO_LOWER=TRUE\ndb.user=sa\n");
    String previous = System.getProperty("enjoytrip.config");
    System.setProperty("enjoytrip.config", file.toString());
    try (Database db = new Database(new Config())) {
      MemberDao members = new MemberDao(db);
      Member member = members.insert("reset@example.com", "회원", Passwords.hash("old-password-123"));
      ResetService reset = new ResetService(db, new Config());
      String old = reset.issue(member.id());
      String current = reset.issue(member.id());
      assertThrows(IllegalArgumentException.class, () -> reset.reset(old, "new-password-123"));
      reset.reset(current, "new-password-123");
      assertTrue(Passwords.verify("new-password-123", members.byId(member.id()).passwordHash()));
      assertTrue(members.byId(member.id()).sessionVersion() > member.sessionVersion());
      assertThrows(
          IllegalArgumentException.class, () -> reset.reset(current, "other-password-123"));
    } finally {
      if (previous == null) System.clearProperty("enjoytrip.config");
      else System.setProperty("enjoytrip.config", previous);
    }
  }
}
