package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.utils.Str;
import java.io.IOException;

/**
 * Folder synchronization with rsync (through rsync.py)
 */
public class RSync {
    private static boolean firstTransfer = true;
    public static String sync(String user, String from, String to, boolean move) throws Exception{
            String host = null;
            if (user.contains("@")){
                String[] tokens = host.split("@");
                user = tokens[0];
                host = tokens[1];
            }
            if ((host==null) || (host.isBlank())) {
                host = "localhost";
            }
            String cmd = String.format("sync_user_data('%s', '%s', '%s', '%s', %s)",
                    user,
                    from,
                    to,
                    host,
                    (move ? "True" : "False"));
            if (firstTransfer){
                Context.getInstance().evalLineBackground("from rsync import sync_user_data");
                firstTransfer = false;
            }
            String ret = Str.toString(Context.getInstance().evalLineBackground(cmd));
            if (!Str.toString(ret).startsWith("Transferred")){
                throw new  IOException(ret);
            }        
            return ret;
    }
    
    
    public static void authorize(String user, boolean fixPermissions) throws Exception{      
        removeAuthorization();
        Context.getInstance().evalLineBackground("from rsync import authorize_user");
        Context.getInstance().evalLine("authorize_user('" + user + "', fix_permissions=" + (fixPermissions?"True":"False") + ")");
    }
    
    public static void removeAuthorization() throws Exception{             
        Context.getInstance().evalLineBackground("from rsync import remove_user_key");
        Context.getInstance().evalLineBackground("remove_user_key()");
    }
    
    public static boolean isAuthorized() throws Exception{    
        Context.getInstance().evalLineBackground("from rsync import is_authorized");
        return (Boolean)Context.getInstance().evalLineBackground("is_authorized()");
    }
        
}
