package ch.psi.pshell.utils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Elog {
    
    public static void log(String logbook, String title, String message) throws Exception {
         log(logbook, title, message, null, null);
    }
    
    public static void log(String logbook, String title, String message,  Map<String, String> atributes) throws Exception {
        log(logbook, title, message, null, atributes);
    }

    public static void log(String logbook, String title, String message, String[] attachments) throws Exception {
        log(logbook, title, message, attachments, null);
    }
    
    public static void log(String logbook, String title, String message, String[] attachments, Map<String, String> atributes) throws Exception {        
        String hostname =  System.getenv().getOrDefault("ELOG_HOST", "elog-gfa.psi.ch") ;            
        String port =  System.getenv().getOrDefault("ELOG_PORT", "443") ;            
        String whenLabel =  System.getenv().getOrDefault("ELOG_DATE_ATTR", "When") ;            
        if ((logbook==null || (logbook.isBlank()))){
            logbook = System.getenv().getOrDefault("ELOG_BOOK", "") ;      
            if ((logbook==null || (logbook.isBlank()))){
                throw new Exception ("Undefined logbook");
            }
        }

        
        long date = Instant.now().getEpochSecond(); //Secondes since epoch
        StringBuffer cmd = new StringBuffer();
        //cmd.append("G_CS_ELOG_add -l \"").append(logbook).append("\" ");
        cmd.append("elog -h \"").append(hostname).append("\" ");
        cmd.append("-p \"").append(port).append("\" ");
        cmd.append("-s "); //SSL
        cmd.append("-u robot robot ");
        cmd.append("-l \"").append(logbook).append("\" ");        
        cmd.append("-a ").append(whenLabel).append("=\"").append(date).append("\" ");        
        
        if (atributes==null){
            atributes = new HashMap<>();
        }
        if (!atributes.containsKey("Author")){
            atributes.put("Author", Sys.getUserName());
        }
        if (!atributes.containsKey("Application")){
            atributes.put("Application", "PShell");
        }
        if (!atributes.containsKey("Category")){
            atributes.put("Category", "Info");
        }

        cmd.append("-a \"Title=").append(title).append("\" ");

        for (String key : atributes.keySet()) {
            String value = atributes.get(key);
            cmd.append("-a ").append(key).append("=\"").append(value).append("\" ");
        }
        
        if (attachments!=null){
            for (String attachment : attachments) {
                cmd.append("-f \"").append(attachment).append("\" ");
            }
        }
        cmd.append("-n 1 ");
        cmd.append("\"").append(message).append("\" ");
        System.out.println(cmd.toString());

        final Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd.toString()});
        new Thread(() -> {
            try {
                process.waitFor();
                int bytes = process.getInputStream().available();
                if (bytes>0){
                    byte[] arr = new byte[bytes];
                    process.getInputStream().read(arr, 0, bytes);
                    System.out.println(new String(arr));
                }
                bytes = process.getErrorStream().available();
                if (bytes>0){
                    byte[] arr = new byte[bytes];
                    process.getErrorStream().read(arr, 0, bytes);
                    System.err.println(new String(arr));
                }
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }).start(); 
    }    
}
