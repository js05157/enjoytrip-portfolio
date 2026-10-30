package com.enjoytrip.member;
import java.util.Locale;
public class MemberService {
 private final MemberDao dao;
 private final String dummy=Passwords.hash("not-an-account-password");
 public MemberService(MemberDao dao){this.dao=dao;}
 public static String email(String value){String email=value.trim().toLowerCase(Locale.ROOT);if(email.length()>254||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("이메일 형식을 확인해 주세요.");return email;}
 public static String name(String value){String name=value.trim();if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException("이름은 1~40자로 입력해 주세요.");return name;}
 public Member register(String email,String name,String password){return dao.insert(email(email),name(name),Passwords.hash(password));}
 public Member login(String email,String password){Member m=dao.byEmail(email.trim().toLowerCase(Locale.ROOT));boolean valid=Passwords.verify(password,m==null?dummy:m.passwordHash());if(!valid||m==null)throw new IllegalArgumentException("이메일 또는 비밀번호를 확인해 주세요.");return m;}
 public Member update(Member m,String email,String name,String current,String next){
  if(!Passwords.verify(current,m.passwordHash()))throw new IllegalArgumentException("현재 비밀번호를 확인해 주세요.");
  return dao.update(m.id(),email(email),name(name),next==null||next.isEmpty()?m.passwordHash():Passwords.hash(next));
 }
 public void delete(Member m,String password){if(!Passwords.verify(password,m.passwordHash()))throw new IllegalArgumentException("현재 비밀번호를 확인해 주세요.");dao.delete(m.id());}
 public Member byId(long id){return dao.byId(id);}
}
