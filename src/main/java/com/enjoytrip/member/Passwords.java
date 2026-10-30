package com.enjoytrip.member;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Passwords {
 private static final int ITERATIONS=600_000;
 private Passwords(){}
 public static String hash(String password){
  validate(password);byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
  return "pbkdf2$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
 }
 public static boolean verify(String password,String stored){
  if(password==null||password.length()>128)return false;
  try{String[] parts=stored.split("\\$");int rounds=Integer.parseInt(parts[1]);if(!parts[0].equals("pbkdf2")||rounds<100_000||rounds>2_000_000)return false;
   return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),derive(password,Base64.getDecoder().decode(parts[2]),rounds));
  }catch(RuntimeException e){return false;}
 }
 public static void validate(String password){if(password==null||password.length()<10||password.length()>128)throw new IllegalArgumentException("비밀번호는 10~128자로 입력해 주세요.");}
 private static byte[] derive(String password,byte[] salt,int rounds){
  PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,rounds,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}catch(GeneralSecurityException e){throw new IllegalStateException(e);}finally{spec.clearPassword();}
 }
}
