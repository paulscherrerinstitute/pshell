package ch.psi.pshell.data;

import ch.psi.pshell.scripting.JepUtils;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
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
        this(name, root, path, data, x, y, z, 3);
    }

    private PlotDescriptor(String name, Object data, double[] x, double[] y, double[] z, int rank) {
        this(name, null, null, data, x, y, z, rank);
    }

    private PlotDescriptor(String name, String root, String path, double[] x, double[] y, double[] z, int rank) {
        this(name, root, path, null, x, y, z, rank);
    }

    private PlotDescriptor(String name, String root, String path, Object data, double[] x, double[] y, double[] z, int rank) {
        this.name = (name == null) ? "" : name;
        if (data instanceof NDArray){
            data = JepUtils.toJavaArray((NDArray)data);
        }
        this.data = data;
        this.root = root;
        this.path = path;
        if (rank < 0) {
            rank = Arr.getRank(data);
            if ((data != null) && (rank == 1)) {
                if ((data instanceof Object[]) && ((((Object[]) data).length) > 0)) {
                    rank = Arr.getRank(((Object[]) data)[0]) + 1;
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
    public int[] steps; //Set for multidimentional data linearized in 1d array
    public int passes;

    public double[] x;
    public double[] y;
    public double[] z;
    public double[] error;

    public boolean unsigned;

    public boolean isMultidimentional1dArray() {
        int[] shape = Arr.getShape(data);
        if (    (rank==2) && (steps != null) && (steps.length >= 2) && 
                (y != null) && (x != null) && (x.length == y.length) &&
                (shape.length >= 2) && (shape[0] == x.length) ){
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

    public String labelX;
}
