package ch.psi.pshell.utils;

import java.io.File;
import java.util.Map;

/**
 *
 */
public class Facility {
    public static String DEFAULT_GROUP = "controls";
    public static String DEFAULT_APP = "pshell";
    
    public static String getName(){
        Map<String, String> env = System.getenv();
        String facility = env.get("FACILITY");
        if ((facility!=null) && (!facility.isBlank())){
            return facility.trim().toLowerCase();
        }
        // Find any key ending with FACILITY
        for (Map.Entry<String, String> e : env.entrySet()) {
            if (e.getKey().endsWith("FACILITY")) {
                facility = e.getValue();
                if ((facility!=null) && (!facility.isBlank())){
                    return facility.trim().toLowerCase();
                }
            }
        }
        return null;
    }
    
    public static File getFolder(String group, String type){
        String facility = getName();
        if (facility != null) {
            File ret = new File("/%s/%s/%s".formatted(facility, group, type));
            return ret.isDirectory() ? ret : null;
        }
        return null;        
    }
    
    public static File getFolder(String group, String type, String app){
        String facility = getName();
        if (facility != null) {
            File ret = new File("/%s/%s/%s/%s".formatted(facility, group, type, app));
            return ret.isDirectory() ? ret : null;
        }
        return null;        
    }

    public static File getConfigFolder(String group){
        return getFolder(group, "config");        
    }
    
    public static File getDataFolder(String group){
        return getFolder(group, "data");        
    }

    public static File getBinFolder(String group){
        return getFolder(group, "bin");        
    }
    
    public static File getAppFolder(String group){
        return getFolder(group, "applications");        
    }

    
    public static File getConfigFolder(String group, String app){
        return getFolder(group, "config", app);        
    }
    
    public static File getAppConfigFolder(){
        return getConfigFolder(DEFAULT_GROUP, DEFAULT_APP);
    }
    
}
