package ch.psi.pshell.data;

import ch.psi.pshell.scripting.JepUtils;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Convert;
import jep.NDArray;

/**
 * Entity class containing information of a plot generated from persisted data.
 */
public class PlotDescriptor {

    public PlotDescriptor(Object data) {
        this("", data);
    }

    public PlotDescriptor(String name) {
        this(name, null);
    }

    public PlotDescriptor(String name, Object data) {
        this(name, data, null);
    }

    public PlotDescriptor(String name, Object data, double[] x) {
        this(name, data, x, null);
    }

    public PlotDescriptor(String name, Object data, double[] x, double[] y) {
        this(name, data, x, y, null, -1);
    }

    public PlotDescriptor(String name, Object data, double[] x, double[] y, double[] z) {
        this(name, data, x, y, z, 3);
    }

    public PlotDescriptor(String name, String root, String path, Object data, double[] x) {
        this(name, root, path, data, x, null);
    }

    public PlotDescriptor(String name, String root, String path, Object data, double[] x, double[] y) {
        this(name, root, path, data, x, y, null, -1);
    }

    public PlotDescriptor(String name, String root, String path, Object data, double[] x, double[] y, double[] z) {
        this(name, root, path, data, x, y, z, Arr.getRank(data)==1 ? -1 :  3);
    }

    private PlotDescriptor(String name, Object data, double[] x, double[] y, double[] z, int rank) {
        this(name, null, null, data, x, y, z, rank);
    }

    private PlotDescriptor(String name, String root, String path, double[] x, double[] y, double[] z, int rank) {
        this(name, root, path, null, x, y, z, rank);
    }

    private PlotDescriptor(String name, String root, String path, Object data, double[] x, double[] y, double[] z, int rank) {
        this.name = (name == null) ? "" : name;
        if (data instanceof NDArray ndarray){
            data = JepUtils.toJavaArray(ndarray);
        }
        this.data = data;
        this.root = root;
        this.path = path;
        if (rank < 0) {
            rank = Arr.getRank(data);
            if ((data != null) && (rank == 1)) {
                if ((data instanceof Object[] arr) && ((((Object[]) data).length) > 0)) {
                    rank = Arr.getRank(arr[0]) + 1;
                }
            }
        }
        this.rank = rank;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean isInMemory() {
        return (data != null) && (rank == Arr.getRank(data));
    }

    public void transpose() {
        Object data = Convert.toDouble(this.data);
        int dataRank = Arr.getRank(data);
        if (dataRank < 2) {
            return;
        }
        if (dataRank == 3) {
            double[][][] arr = (double[][][]) data;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = Convert.transpose(arr[i]);
            }
            this.data = arr;
        } else if (dataRank == 2) {
            this.data = Convert.transpose(data);
        }
        //double[] aux = x;
        //x = y;
        //y = aux;
    }

    public String name;
    public String root;
    public String path;

    public Object data;
    public int rank;
    public int passes;
    public int dimensions;
    
    //Set for multidimentional data linearized in 1d array
    public int[] steps; 
    public double[] start; 
    public double[] end;         
    public boolean zigzag; 
    

    public double[] x;
    public double[] y;
    public double[] z;
    public double[] error;

    public boolean unsigned;

    public boolean isMultidimentional1dArray() {
        int[] shape = Arr.getShape(data);
        if (    (rank<=2) && (steps != null) && (steps.length >= 2) && 
                (y != null) && (x != null) && (x.length == y.length) &&
                (shape.length >= 1) && (shape[0] == x.length) ){
            if (steps.length==2){
                return true;
            }
            if (steps.length==3){
                if ((z != null) && (z.length == shape[0])){
                    return true;
                }
            }
        }
       return false;
    }


    public double getStepSize(int dim){
        if ((steps!=null) && (end!=null) && (start!=null)){
            if ((dim<steps.length) && (dim<end.length)  && (dim<start.length) ){
                return steps[dim]!=0 ? (end[dim] - start[dim]) / (steps[dim]) : 0;
            }
        }
        return Double.NaN;
    }
    
    public double getStepSizeX(){
        if ((steps!=null) && (steps.length>0)){
            return getStepSize(steps.length-1);
        }
        return Double.NaN;
    }

    public double getStepSizeY(){
        if ((steps!=null) && (steps.length>1)){
            return getStepSize(steps.length-2);
        }
        return Double.NaN;
    }

    public double getStepSizeZ(){
        if ((steps!=null) && (steps.length>2)){
            return getStepSize(steps.length-3);
        }
        return Double.NaN;
    }
    
     
    public String[] labels;

    public String getLabel(int dim){
        if ((labels!=null) &&  (dim<labels.length) ){
            return labels[dim];
        }
        return null;
    }
    
    public String getLabelX(){
        if ((labels!=null) && (labels.length>0)){
            return labels[labels.length-1];
        }
        return "x";
    }
    
    public String getLabelY(){
        if ((labels!=null) && (labels.length>1)){
            return labels[labels.length-2];
        }
        return "y";
    }

    public String getLabelZ(){
        if ((labels!=null) && (labels.length>2)){
            return labels[labels.length-3];
        }
        return "z";
    }
    
    public void setLabelX(String value){
        if ((labels!=null) && (labels.length>0)){
            labels[labels.length-1] = value;
        } else {
            labels=new String[]{value};
        }
    }
    
    public void setLabelY(String value){
        if ((labels!=null) && (labels.length>1)){
            labels[labels.length-2] = value;
        } else {
            labels=new String[]{value,""};
        }
    }
    
    public void setLabelZ(String value){
        if ((labels!=null) && (labels.length>2)){
            labels[labels.length-3] = value;
        } else {
            labels=new String[]{value,"",""};
        }
    }
        
    
}
