package ch.psi.pshell.data;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.sequencer.ExecutionParameters;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class LayoutFDA extends LayoutTable {
    static String filePrefix = null;
    
    @Override
    public String getId() {
        return "fda";
    }    

    public static String getFilePrefix(){
        if (filePrefix == null){
            if (!Layout.isFlatStorage()){
                String dataPath = Context.getDataFilePattern();
                try{
                    dataPath = dataPath.replaceAll("./$", ""); //remove last / if present
                    String[] tokens  = dataPath.split("/");
                    return tokens[tokens.length-1];                
                } catch (Exception ex){                    
                }
            }
            return "{date}_{time}_{name}";            
        }
        return filePrefix;
    }

    public static void setFilePrefix(String filePrefix){
        LayoutFDA.filePrefix = filePrefix;
    }
    
    
    @Override
    protected String getLogFileName() {        
        if (!Layout.isFlatStorage()){
           return  super.getLogFileName();
        }        
        ExecutionParameters pars = Context.getExecutionPars();
        return Setup.expandPath(getFilePrefix(), pars.getStart())+".log";
    }
    
    @Override
    protected String getDatasetName(Scan scan) {        
        ExecutionParameters pars =Context.getExecutionPars();
        if (!Layout.isFlatStorage()){
             return  "/" + Context.getDefaultScanTag(pars.getCount()); 
        }
        return  Setup.expandPath(getFilePrefix()+ "_" + String.format("%04d",(pars.getCount()-1)), pars.getStart());                        
    }    
    
    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        if (FormatFDA.matches(root, path, dm, null)){
            throw new IOException("Let FDA make the plotting");
        }
        return super.getScanPlots(root, path, dm);
    }    
}

