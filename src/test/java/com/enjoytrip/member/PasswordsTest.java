package com.enjoytrip.member;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordsTest {
  @Test
  void saltedPasswordsValidateWithoutStoringPlaintext() {
    String hash = Passwords.hash("correct-password-123");
    assertTrue(Passwords.verify("correct-password-123", hash));
    assertFalse(Passwords.verify("wrong-password-123", hash));
    assertNotEquals(hash, Passwords.hash("correct-password-123"));
    assertFalse(hash.contains("correct-password"));
  }

  @Test
  void refusesInvalidPasswordsAndHashes() {
    assertThrows(IllegalArgumentException.class, () -> Passwords.hash("short"));
    assertFalse(Passwords.verify("password", "invalid"));
  }
}
