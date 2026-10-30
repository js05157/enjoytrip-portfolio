package com.enjoytrip.plan;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.attraction.*;
import com.enjoytrip.common.*;
import com.enjoytrip.member.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class PlanDaoTest {
  @TempDir Path temp;

  @Test
  void checksOwnershipReorderingAndDayBounds() throws Exception {
    Path f = temp.resolve("db.properties");
    Files.writeString(
        f, "db.url=jdbc:h2:mem:plans;MODE=MySQL;DATABASE_TO_LOWER=TRUE\ndb.user=sa\n");
    String previous = System.getProperty("enjoytrip.config");
    System.setProperty("enjoytrip.config", f.toString());
    try (Database db = new Database(new Config())) {
      MemberDao members = new MemberDao(db);
      long owner = members.insert("owner@example.com", "주인", "test-hash").id(),
          other = members.insert("other@example.com", "다른 회원", "test-hash").id();
      PlanDao dao = new PlanDao(db);
      long id =
          dao.create(
              owner, "서울 여행", LocalDate.now(), LocalDate.now().plusDays(1), BigDecimal.TEN, "메모");
      Attraction a =
          new TourClient(new Config())
              .search(new AttractionQuery("1", "", "12", "", 1, 12, "A"))
              .items()
              .get(0);
      dao.add(id, owner, 1, a, BigDecimal.ONE, "");
      dao.add(id, owner, 1, a, BigDecimal.TEN, "두 번째");
      assertNull(dao.get(id, other));
      assertThrows(IllegalArgumentException.class, () -> dao.stops(id, other));
      var stops = dao.stops(id, owner);
      dao.reorder(id, owner, List.of(stops.get(1).id(), stops.get(0).id()));
      assertEquals(stops.get(1).id(), dao.stops(id, owner).get(0).id());
      assertThrows(
          IllegalArgumentException.class,
          () -> dao.reorder(id, owner, List.of(stops.get(0).id(), stops.get(0).id())));
      assertThrows(
          IllegalArgumentException.class, () -> dao.add(id, owner, 3, a, BigDecimal.ZERO, ""));
      dao.delete(id, other);
      assertNotNull(dao.get(id, owner));
      dao.delete(id, owner);
      assertNull(dao.get(id, owner));
    } finally {
      if (previous == null) System.clearProperty("enjoytrip.config");
      else System.setProperty("enjoytrip.config", previous);
    }
  }
}
