package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.core.ExecutionParameters;
import ch.psi.pshell.scan.Scan;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 *
 */
public class LayoutFDA extends LayoutTable {
    static String filePrefix = null;

    public static boolean isFlatStorage(){
        String dataPath = Context.getInstance().getConfig().dataPath;
        return !dataPath.contains("{name}");
    }

    public static String getFilePrefix(){
        if (filePrefix == null){
            if (!isFlatStorage()){
                String dataPath = Context.getInstance().getConfig().dataPath;
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
        ExecutionParameters pars = Context.getInstance().getExecutionPars();
        return Context.getInstance().getSetup().expandPath(getFilePrefix(), pars.getStart())+".log";
    }
    
    @Override
    protected String getDatasetName(Scan scan) {        
        ExecutionParameters pars = Context.getInstance().getExecutionPars();
        return  Context.getInstance().getSetup().expandPath(getFilePrefix()+ "_" + String.format("%04d",(pars.getCount()-1)), pars.getStart());                        
    }    
    
    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        if (isFdaDataFile(root, path, dm, null)){
            throw new IOException("Let FDA make the plotting");
        }
        return super.getScanPlots(root, path, dm);
    }
    
    public static boolean isFdaDataFile(String root, String path) throws IOException{
        return isFdaDataFile(root, path, (Context.isDataManagerInstantiated()) ? Context.getInstance().getDataManager() : null, null);
    }
    
    static boolean isFdaDataFile(String root, String path, DataManager dm, Provider p) throws IOException{
        if (dm!=null){
            if (!dm.isDataset(root, path)) {
                return false;
            }        
            if (p==null){
                p = dm.getProvider();
            } 
        }       
        if ((p!=null) && (p instanceof ProviderText)){
            Path filePath = ((ProviderText)p).getFilePath(root, path);
            try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                String first = br.readLine(); 
                String second = br.readLine();
                String third = br.readLine();
                if ((first.startsWith("#")) && (second.startsWith("#")) && (!third.startsWith("#"))){
                    for (String token: second.substring(1).split(((ProviderText)p).getItemSeparator())){
                        Integer.valueOf(token.trim());
                    }
                    return true;
                }
            } catch (Exception ex) {            
            }
        }
        return false;
    }
    
    /*
    //FDA layout doesn' save global attributes
    @Override
    public void onOpened(File output) throws IOException {
    } 
    
    @Override
    public void onClosed(File output) throws IOException {
    }     
    */
}

