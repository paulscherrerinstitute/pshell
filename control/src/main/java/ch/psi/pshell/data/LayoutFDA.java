package ch.psi.pshell.data;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.scan.Scan;
import ch.psi.pshell.sequencer.ExecutionParameters;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
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

    public static boolean isFlatStorage(){
        String dataPath = Context.getDataFilePattern();
        return !dataPath.contains("{name}") 
                && !dataPath.contains("{time}") 
                && !dataPath.contains("{sec}") 
                && !dataPath.contains("{seq}") 
                && !dataPath.contains("{dseq}") ;
    }

    public static String getFilePrefix(){
        if (filePrefix == null){
            if (!isFlatStorage()){
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
        if (!isFlatStorage()){
           return  super.getLogFileName();
        }        
        ExecutionParameters pars = Context.getExecutionPars();
        return Setup.expandPath(getFilePrefix(), pars.getStart())+".log";
    }
    
    @Override
    protected String getDatasetName(Scan scan) {        
        ExecutionParameters pars =Context.getExecutionPars();
        if (!isFlatStorage()){
             return  String.format("/scan %d", pars.getCount());
        }
        return  Setup.expandPath(getFilePrefix()+ "_" + String.format("%04d",(pars.getCount()-1)), pars.getStart());                        
    }    
    
    @Override
    public List<PlotDescriptor> getScanPlots(String root, String path, DataManager dm) throws IOException {
        if (isFdaDataFile(root, path, dm, null)){
            throw new IOException("Let FDA make the plotting");
        }
        return super.getScanPlots(root, path, dm);
    }
    
    public static boolean isFdaDataFile(Path filePath) throws IOException{
        return isFdaDataFile(filePath, null);
    }
    
    public static boolean isFdaDataFile(Path filePath, Format p) throws IOException{
        String separator = (p==null) ? FormatFDA.ITEM_SEPARATOR: ((FormatText)p).getItemSeparator();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String first = br.readLine(); 
            String second = br.readLine();
            String third = br.readLine();
            if ((first.startsWith("#")) && (second.startsWith("#")) && (!third.startsWith("#"))){
                for (String token: second.substring(1).split(separator)){
                    Integer.valueOf(token.trim());
                }
                return true;
            }
        } catch (Exception ex) {            
        }
        return false;
    }
    
    public static boolean isFdaDataFile(String root, String path) throws IOException{
        return isFdaDataFile(root, path, Context.getDataManager(), null);
    }
    
    static boolean isFdaDataFile(String root, String path, DataManager dm, Format p) throws IOException{
        if (dm!=null){
            if (!dm.isDataset(root, path)) {
                return false;
            }        
            if (p==null){
                p = dm.getFormat();
            } 
        }       
        if (p instanceof FormatText providerText){
            Path filePath = providerText.getFilePath(root, path);
            return isFdaDataFile(filePath, p);
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

