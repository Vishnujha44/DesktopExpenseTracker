package com.expensetracker.util;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.security.NoSuchAlgorithmException;
public class SecurityUtil {
    public static String sha256(String input){
        try{ MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] h=md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb=new StringBuilder();
            for(byte b:h) sb.append(String.format("%02x",b));
            return sb.toString();
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }
}
