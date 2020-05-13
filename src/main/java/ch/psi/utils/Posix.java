package ch.psi.utils;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import java.util.Arrays;
import java.util.List;


/**
 *
 */
public class Posix{

    public static final int OK = 0;
    public static final int ERROR = -1;

    public static int getuid() {
        return CLibrary.INSTANCE.getuid();
    }
    
    public static int setuid(int uid) {
        return CLibrary.INSTANCE.setuid(uid);
    }
    
    public static int geteuid() {
        return CLibrary.INSTANCE.geteuid();
    }    

    public static int seteuid(int euid) {
        return CLibrary.INSTANCE.seteuid(euid);
    }
    
    public static int getgid() {
        return CLibrary.INSTANCE.getgid();
    }  
    
    public static int setgid(int gid) {
        return CLibrary.INSTANCE.setgid(gid);
    }

    public static int getegid() {
        return CLibrary.INSTANCE.getegid();
    }  
    
    public static int setegid(int gid) {
        return CLibrary.INSTANCE.setegid(gid);
    }
       
    public static int setumask(int umask) {
        return CLibrary.INSTANCE.umask(umask);
    }
    
    public static Passwd getpwnam(String userName) {
        return CLibrary.INSTANCE.getpwnam(userName);
    }    
    
    public static class Passwd extends Structure {

        public Passwd() {
        }

        public Passwd(Pointer p) {
            super(p);
        }
        
        public String name;
        public String passwd;
        public int uid;
        public int gid;
        //...


        public String getName(){
            return name;
        }

        public String getPasswd() {
            throw new UnsupportedOperationException();
        }

        public int getUID() {
            return uid;
        }

        public int getGID() {
            return gid;
        }
        
        public int getPasswdChangeTime(){
            throw new UnsupportedOperationException();
        }
        
        public String getAccessClass(){
            throw new UnsupportedOperationException();
        }
        
        public String getGECOS(){
            throw new UnsupportedOperationException();
        }
        
        public String getHome(){            
            throw new UnsupportedOperationException();
        }
        
        public String getShell(){
            throw new UnsupportedOperationException();
        }
        
        public int getExpire(){
            throw new UnsupportedOperationException();
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("name", "passwd", "uid", "gid");
        }
    }
    
    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);

        int getuid();
        
        int setuid(int uid);

        int geteuid();

        int seteuid(int euid);
        
        int getgid();

        int setgid(int gid);

        int getegid();

        int setegid(int gid);
        
        int umask(int umask);
        
        Passwd getpwnam(String userName); 
    }
}