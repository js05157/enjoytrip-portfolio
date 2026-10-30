package com.enjoytrip.member;

public record Member(long id, String email, String name, String passwordHash, int sessionVersion) {
  public long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }
}
