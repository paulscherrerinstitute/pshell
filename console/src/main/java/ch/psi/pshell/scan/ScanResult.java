package ch.psi.pshell.scan;

import ch.psi.pshell.data.LayoutDefault;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Writable;
import ch.psi.pshell.scripting.Subscriptable;
import ch.psi.pshell.scripting.Subscriptable.SubscriptableList;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.Nameable;
import ch.psi.pshell.utils.Reflection.Hidden;
import java.beans.Transient;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.util.MultidimensionalCounter;

/**
 * ScanResult objects package all the acquired data during a scan.
 */
public class ScanResult implements SubscriptableList<ScanRecord>, Subscriptable.Dict<Object,Object>, Nameable{

    final List<ScanRecord> records;
    final Scan scan;

    ScanResult(Scan scan) {
        this.scan = scan;
        records = new ArrayList<>();
    }

    public List<ScanRecord> getRecords() {
        return records;
    }
    
    public boolean getInMemory(){
        return scan.getKeep();
    }
    
    public String getDataFormat(){
        if (scan.getDataFormat()== null){
            return "";
        }
        return scan.getDataFormat().getClass().getName();
    }    
    
    public String getDataLayout(){
        if (scan.getDataLayout()== null){
            return "";
        }        
        return scan.getDataLayout().getClass().getName();
    }      

    public List<Object> getReadable(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getReadableIndex(id);
        if ((index < 0) || (index >= getReadables().size())) {
            throw new IllegalArgumentException("Index");
        }
        if(getInMemory()) {
            List ret = new ArrayList<>();
            for (ScanRecord record : records) {
                ret.add(record.values[index]);
            }
            return ret;
        } else {
            return asList(scan.readData(scan.getReadableNames()[index]));
        }
    }

    public List<Number> getSetpoints(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getWritableIndex(id);
        if ((index < 0) || (index >= getWritables().size())) {
            throw new IllegalArgumentException("Index");
        }
        if(getInMemory()) {
            ArrayList<Number> ret = new ArrayList<>();
            for (ScanRecord record : records) {
                ret.add(record.setpoints[index]);
            }
            return ret;
        } else {
            return asList(scan.readData(scan.getWritableNames()[index]+ LayoutDefault.SETPOINTS_DATASET_SUFFIX));
        }
    }

    public List<Number> getPositions(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getWritableIndex(id);
        if ((index < 0) || (index >= getWritables().size())) {
            throw new IllegalArgumentException("Index");
        }
        if(getInMemory()) {
            ArrayList<Number> ret = new ArrayList<>();
            for (ScanRecord record : records) {
                ret.add(record.positions[index]);
            }
            return ret;
        } else {
            return  asList(scan.readData(scan.getWritableNames()[index]));
        }
    }
    
    public List<Object> getMonitor(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getMonitorIndex(id);
        if ((index < 0) || (index >= getMonitors().size())) {
            throw new IllegalArgumentException("Index");
        }
        return asList(scan.readMonitor(scan.getMonitorNames()[index]));
    }
    
    public List<Object> getDiag(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getDiagIndex(id);
        if ((index < 0) || (index >= getDiags().size())) {
            throw new IllegalArgumentException("Index");
        }
        return asList(scan.readDiag(scan.getDiagNames()[index]));
    }    
    
    public Object getSnap(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getSnapIndex(id);
        if ((index < 0) || (index >= getSnaps().size())) {
            throw new IllegalArgumentException("Index");
        }
        Object ret = scan.readSnap(scan.getSnapsNames()[index]);
        if (ret.getClass().isArray()){
            return asList(ret);
        }
        return ret;
    }    

    public List<Long> getTimestamps() {
        return asList(scan.readTimestamps());
    }       

    public List getDevice(Object id) {
        int index = (id instanceof Number number) ? number.intValue() : scan.getDeviceIndex(id);
        if (index<getReadables().size()) {
            return getReadable(index);
        }
        index-=getReadables().size();
        if (index<getWritables().size()) {
            return getPositions(index);
        }
        index-=getWritables().size();
        if (index<getMonitors().size()) {
            return getMonitor(index);
        }
        index-=getMonitors().size();
        if (index<getDiags().size()) {
            return getDiag(index);
        }
        index-=getDiags().size();
        if (index<getSnaps().size()) {
            Object ret = getSnap(index);
            if (ret instanceof List list){
                return list;
            }
            return asList(ret);
        }        
        throw new IllegalArgumentException("Index");
    }

    public int getSize() {
        return records.size();
    }

    private List asList(Object obj){
        if (obj!=null){
            if (obj instanceof List list){
                return list;
            }
            if (!obj.getClass().isArray()){
                obj = new Object[]{obj};
            }
            return Arr.toList(obj);        
        }
        return new ArrayList();        
    }
    
    @Transient
    public List<Readable> getReadables() {
        return asList(scan.getReadables());
    }

    @Transient
    public List<Writable> getWritables() {
        return asList(scan.getWritables());
    }

    @Transient
    public List<Device> getMonitors() {
        return asList(scan.getMonitors());
    }
    
    @Transient
    public List<Readable> getDiags() {
        return asList(scan.getDiags());
    }    

    @Transient
    public List<Readable> getSnaps() {
        return asList(scan.getSnaps());
    }    
    
    @Transient
    public java.util.Map<String, Object> getMeta() {        
        return scan.getMeta();
    }      
        
    public java.util.Map<Readable, Object> getSnapValues() {
        java.util.Map<Readable, Object> ret = new HashMap <>();
        if (scan.getSnaps()!=null){
            ret = scan.readSnaps();
        }
        return ret;
    }        
    
    @Transient
    public List<Nameable> getDevices() {
        return asList(scan.getDevices());
    }
    
    public List<String> getWritableNames() {
        return asList(scan.getWritableNames());
    }
    
    public List<String> getReadableNames() {
        return asList(scan.getReadableNames());
    }  
    
    public List<String> getMonitorNames() {
        return asList(scan.getMonitorNames());
    }      
    
    public List<String> getDiagNames() {
        try{
        return asList(scan.getDiagNames());
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ArrayList<String>();
    }      
    
    public List<String> getSnapsNames() {
        return asList(scan.getSnapsNames());
    }        

    public int getIndex() {
        return scan.getIndex();
    }

    public String getRoot() {
        return (scan.getPath() == null) ? null : getPath().split("\\|")[0].trim();
    }

    public String getGroup() {
        return (scan.getPath() == null) ? null : getPath().split("\\|")[1].trim();
    }

    public String getPath() {
        return scan.getPath();
    }
    
    public int getDimensions() {
        return scan.getDimensions();
    }

    public String print() {
        return print("\t");
    }

    int errorCode = 0;

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a table of the scan results: the column names are "Time",
     * "Index", positioners' names and sensors' names.
     */
    public String print(String separator) {
        String lineSeparator = "\n";
        StringBuilder sb = new StringBuilder();

        sb.append(scan.getHeader(separator)).append(lineSeparator);
        ArrayList<String> data = new ArrayList<>();
        for (ScanRecord record : records) {
            sb.append(record.print(separator)).append(lineSeparator);
        }
        return sb.toString();
    }

    boolean completed;

    public boolean isCompleted() {
        return completed;
    }

    public long getTimeElapsed() {
        return scan.getTimeElapsed();
    }

    @Transient
    public Scan getScan() {
        return scan;
    }

    /**
     * Return a multidimensional array holding the scan data for the readable
     * given by index, with same type as readable and dimensions = passes x
     * [scan dimensions]. Do not manage zigzag.
     */
    public Object getData(int index) {
        int[] steps = scan.getNumberOfSteps();
        int numDimensions = steps.length + 1;
        int[] dimensions = new int[numDimensions];

        dimensions[0] = scan.getNumberOfPasses();
        for (int i = 0; i < steps.length; i++) {
            dimensions[i + 1] = steps[i] + 1;
        }

        MultidimensionalCounter mc = new MultidimensionalCounter(dimensions);

        //ArrayList ret = new ArrayList(); 
        if (records.size() == 0) {
            return null;
        }

        Class type = records.get(0).values[index].getClass();
        if (Convert.isWrapperClass(type)) {
            type = Convert.getPrimitiveClass(type);
        }
        Object ret = Array.newInstance(type, dimensions);

        for (ScanRecord rec : records) {
            int[] pos = mc.getCounts(rec.index);
            Object val = rec.values[index];

            //TODO: Optimize me
            Object lowerDim = ret;
            int i = 0;
            for (; i < numDimensions - 1; i++) {
                lowerDim = Array.get(lowerDim, pos[i]);
            }
            Array.set(lowerDim, pos[i], val);
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (scan != null) {
            sb.append(scan.toString() + ": ");
            sb.append("records=").append(getSize()).append("/").append(scan.getNumberOfRecords());
             //TODO: Cannot get names in single thread interpreters
            sb.append(" positioners=[").append(String.join(",", getWritableNames())).append("],");
            sb.append(" sensors=[").append(String.join(",", getReadableNames())).append("]");
        }
        return sb.toString();
    }

    //SubscriptableArray interface
    @Hidden
    @Override
    @Transient
    public List getValues() {
        return records;
    }

    //Dict interface
    @Hidden
    @Override
    public Object __getitem__(Object key) {
        return getDevice(key);
    }

    @Override
    public List<Object> keys() {
        return Arrays.asList(Convert.toObjectArray(scan.getDeviceNames()));
    }
}
