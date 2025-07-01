package ch.psi.pshell.scan;

import ch.psi.pshell.data.Layout;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.framework.Context;
import java.io.IOException;
import ch.psi.pshell.data.Format;

/**
 *
 */
public class DataAccessDummyScan extends ScanBase{
    final Layout layout;
    final Format format;
    final String path;
    final String group;

    public DataAccessDummyScan(Format format, Layout layout, String path, String group) {
        super(new Writable[0], new ch.psi.pshell.device.Readable[0], new double[0], new double[0], new int[0],false,0,1,false);
        this.layout = (layout==null) ? Context.getLayout():layout;
        this.format= (format==null) ? Context.getFormat(): format;
        this.path=path;
        String[] aux = path.split("\\|");
        this.group=(group==null) ? ( (aux.length>0) ? aux[aux.length-1] : ""): group;
    }

    @Override
    public Layout getDataLayout(){
        return layout;
    }

    @Override
    public Format getDataFormat(){
        return format;
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
      
      
    public static Object readScanData(Format format, Layout layout, String path, String group, String device){  
        DataAccessDummyScan scan = new DataAccessDummyScan (format, layout, path, group);
        return scan.readData(device);
    }  

}
