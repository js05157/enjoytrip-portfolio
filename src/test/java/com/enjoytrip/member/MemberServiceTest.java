package com.enjoytrip.member;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.common.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class MemberServiceTest {
  @TempDir Path temp;

  @Test
  void handlesRegistrationDuplicateEmailUpdateAndDeletion() throws Exception {
    Path file = temp.resolve("test.properties");
    Files.writeString(
        file,
        "app.mode=demo\n"
            + "db.url=jdbc:h2:mem:members;MODE=MySQL;DATABASE_TO_LOWER=TRUE\n"
            + "db.user=sa\n");
    String previous = System.getProperty("enjoytrip.config");
    System.setProperty("enjoytrip.config", file.toString());
    try (Database db = new Database(new Config())) {
      MemberService service = new MemberService(new MemberDao(db));
      Member member = service.register("User@Example.com", "여행자", "password-12345");
      assertEquals("user@example.com", member.email());
      assertThrows(
          IllegalArgumentException.class,
          () -> service.register("user@example.com", "다른 사람", "password-12345"));
      assertThrows(
          IllegalArgumentException.class, () -> service.login("user@example.com", "incorrect"));
      assertEquals(member.id(), service.login("user@example.com", "password-12345").id());
      assertThrows(
          IllegalArgumentException.class,
          () -> service.update(member, "next@example.com", "새 이름", "incorrect", ""));
      Member next =
          service.update(
              member, "next@example.com", "새 이름", "password-12345", "new-password-12345");
      assertTrue(next.sessionVersion() > member.sessionVersion());
      assertEquals("새 이름", service.login("next@example.com", "new-password-12345").name());
      service.delete(next, "new-password-12345");
      assertNull(service.byId(member.id()));
    } finally {
      if (previous == null) System.clearProperty("enjoytrip.config");
      else System.setProperty("enjoytrip.config", previous);
    }
  }
}
