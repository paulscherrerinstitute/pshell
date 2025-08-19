package ch.psi.pshell.devices;

import ch.psi.pshell.archiver.Daqbuf;
import ch.psi.pshell.camserver.CameraClient;
import ch.psi.pshell.camserver.PipelineClient;
import java.nio.file.Paths;

/**
 * Entity class holding the application defaults and command-line options, folder structure 
 * and providing file name expansion.
 */
public class Setup extends ch.psi.pshell.app.Setup {    
      
            
    public static boolean isSimulation() {
        return Options.SIMULATION.getBool(Boolean.FALSE);
    }  
        
    public static boolean isEmptyPool(){
        return Options.EMPTY.getBool(Boolean.FALSE);
    }
    
    public static boolean isParallelInit(){
        return Options.PARALLEL.getBool(Boolean.FALSE);
    }
        
    public static String getPipelineServer() {
        return  Options.PIPELINE_SERVER.getString(PipelineClient.DEFAULT_URL);
    }    
    
    public static String getCameraServer() {
        return Options.CAMERA_SERVER.getString(CameraClient.DEFAULT_URL);
    }        

    public static String getArchiver() {
        return Options.ARCHIVER.getString(Daqbuf.getDefaultUrl());
    }        
    
    public static String getBackend() {
        return Options.BACKEND.getString(Daqbuf.getDefaultBackend());
    }            
    
    public static String getEpicsConfig() {
        return Options.EPICS_CONFIG.getString(null);
    }                                   
    
    public static String getCachePathRenderers(){
        return getCachePath("renderers");
    }        
}
