package com.enjoytrip.member;

import static org.junit.jupiter.api.Assertions.*;

import com.enjoytrip.common.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class MailDeliveryTest {
  @Test
  void missingMailConfigurationIsDetectedBeforeLookingUpAnAccount() {
    var service =
        new ResetService(
            null, new Config(Map.of("app.mode", "live", "smtp.host", "", "smtp.from", "")));
    assertFalse(service.available());
    assertThrows(IllegalStateException.class, () -> service.request("anyone@example.com"));
  }

  @Test
  void liveModeSendsResetMailWithoutExposingLinkToBrowser() throws Exception {
    try (ServerSocket smtp = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      CompletableFuture<String> received = new CompletableFuture<>();
      Thread worker =
          new Thread(
              () -> {
                try (Socket socket = smtp.accept();
                    BufferedReader reader =
                        new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter out =
                        new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
                  out.println("220 local-test ESMTP");
                  boolean data = false;
                  StringBuilder message = new StringBuilder();
                  String line;
                  while ((line = reader.readLine()) != null) {
                    if (data) {
                      if (line.equals(".")) {
                        data = false;
                        received.complete(message.toString());
                        out.println("250 accepted");
                      } else message.append(line).append('\n');
                    } else if (line.startsWith("EHLO") || line.startsWith("HELO"))
                      out.println("250 local-test");
                    else if (line.equals("DATA")) {
                      data = true;
                      out.println("354 send data");
                    } else if (line.equals("QUIT")) {
                      out.println("221 bye");
                      break;
                    } else out.println("250 ok");
                  }
                } catch (Exception e) {
                  received.completeExceptionally(e);
                }
              });
      worker.setDaemon(true);
      worker.start();
      Config config =
          new Config(
              Map.of(
                  "app.mode",
                  "live",
                  "db.url",
                  "jdbc:h2:mem:mail;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                  "db.user",
                  "sa",
                  "smtp.host",
                  "127.0.0.1",
                  "smtp.port",
                  "" + smtp.getLocalPort(),
                  "smtp.from",
                  "noreply@example.com",
                  "smtp.starttls",
                  "false"));
      try (Database db = new Database(config)) {
        new MemberDao(db).insert("recipient@example.com", "수신자", "test-hash");
        assertNull(new ResetService(db, config).request("recipient@example.com"));
        String wire = received.get(5, TimeUnit.SECONDS);
        var message =
            new jakarta.mail.internet.MimeMessage(
                jakarta.mail.Session.getInstance(new java.util.Properties()),
                new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)));
        assertTrue(message.getContent().toString().contains("/auth/reset?token="));
      }
      worker.join(1000);
    }
  }
}
