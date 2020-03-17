package ch.psi.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;


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
    }
}