package ch.psi.pshell.pkg;

import ch.psi.pshell.pkg.Package;
import ch.psi.pshell.utils.Convert;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PackageManager implements AutoCloseable{
    public static  PackageManager INSTANCE; 
    
    public static PackageManager getInstance(){
        if (INSTANCE == null){
            throw new RuntimeException("Package Manager not instantiated.");
        }        
        return INSTANCE;
    }
    
    public static boolean hasInstance(){
        return INSTANCE!=null;
    }    
        
    final List<Package> packages;
        
    public PackageManager(File[] packagePaths){
        this(Convert.toStringArray(packagePaths));
    }
    
    public PackageManager(String[] packagePaths){
        INSTANCE = this;
        packages = new ArrayList<>();                
        for (String path: packagePaths){
            try {
                packages.add(new Package(path));
            } catch (IOException ex) {
                Logger.getLogger(PackageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public List<String> getPackagePaths(){
        List<String> ret = new ArrayList<>();
        for (Package p:getPackages()){
            ret.add(p.path);
        }
        return ret;
    }

    public Package[] getPackages(){
        return packages.toArray(new Package[0]);
    }
    
    public void loadExtensionsFolders() {
        for (Package p:packages){
            try {
                p.loadExtensionsFolder();
            } catch (Exception ex) {
                Logger.getLogger(PackageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }
    
    public void initialize(){
        for (Package p:packages){
            try {
                p.open();
            } catch (Exception ex) {
                Logger.getLogger(PackageManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (Package p:packages){
            try {
                p.close();
            } catch (Exception ex) {
                Logger.getLogger(PackageManager.class.getName()).log(Level.WARNING, null, ex);
            }
        }        
    }
    
}
