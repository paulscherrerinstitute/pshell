package ch.psi.pshell.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PackageManager implements AutoCloseable{
    
    
    final List<Package> packages;
    
    public PackageManager(Setup  setup, String[] packagePaths){
        packages = new ArrayList<>();                
        for (String path: packagePaths){
            try {
                packages.add(new Package(setup, path));
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
    
    void loadExtensionsFolders() {
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
