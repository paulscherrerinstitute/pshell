package ch.psi.pshell.scan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.Layout;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.device.Writable;
import java.io.IOException;

/**
 *
 */
public class DataAccessDummyScan extends ScanBase{
    final Layout layout;
    final Provider provider;
    final String path;
    final String group;

    public DataAccessDummyScan(Provider provider, Layout layout, String path, String group) {
        super(new Writable[0], new ch.psi.pshell.device.Readable[0], new double[0], new double[0], new int[0],false,0,1,false);
        this.layout = (layout==null) ? Context.getInstance().getDataManager().getLayout():layout;
        this.provider= (provider==null) ? Context.getInstance().getDataManager().getProvider(): provider;
        this.path=path;
        String[] aux = path.split("\\|");
        this.group=(group==null) ? ( (aux.length>0) ? aux[aux.length-1] : ""): group;
    }

    @Override
    public Layout getDataLayout(){
        return layout;
    }

    @Override
    public Provider getDataProvider(){
        return provider;
    }  

    @Override
    public String getPath(){
        return path;
    } 

    public String getGroup(){
        return group;
    }         

    @Override
    protected void doScan() throws IOException, InterruptedException {
    } 
      
      
    public static Object readScanData(Provider provider, Layout layout, String path, String group, String device){  
        DataAccessDummyScan scan = new DataAccessDummyScan (provider, layout, path, group);
        return scan.readData(device);
    }  

}
