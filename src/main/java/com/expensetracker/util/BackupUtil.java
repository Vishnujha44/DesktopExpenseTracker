package com.expensetracker.util;
import java.io.*;
public class BackupUtil {
    public static boolean backupDatabase(String dbPath, String destPath){
        try (InputStream in = new FileInputStream(dbPath);
             OutputStream out = new FileOutputStream(destPath)) {
            byte[] buf = new byte[8192]; int len;
            while((len=in.read(buf))>0) out.write(buf,0,len);
            return true;
        }
        catch (Exception e){ e.printStackTrace();
            return false;
        }
    }
}
