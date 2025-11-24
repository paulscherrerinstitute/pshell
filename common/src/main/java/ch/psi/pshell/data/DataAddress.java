
package ch.psi.pshell.data;

/**
 *
 */
public class DataAddress {
    
    DataAddress(String root, String path) {
        this.root = root;
        this.path = path;
    }
    public final String root;
    public final String path;
    
    static public boolean isFullPath(String path){
        return path.contains("|") || path.contains(" /");
    }
    
    static public DataAddress fromFullPath(String fullPath){  
        String[] tokens = fullPath.contains("|") ? fullPath.split("\\|") : fullPath.split(" /");
        String root = tokens[0].trim();
        String path = (tokens.length > 1) ? tokens[1].trim() : "/";
        return new DataAddress(root, path);
        
    }
    
     static public DataAddress fromRoot(String root){
         return new DataAddress(root, "/");
     }
     
     static public DataAddress fromDataStore(DataStore ds, String path){
         return new DataAddress(ds.getOutput(), ((path==null)||(path == "")) ? "/" : path);
     }
     
}
