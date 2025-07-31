package ch.psi.pshell.data;

import ch.psi.pshell.utils.Arr;
import java.lang.reflect.Array;

/**
 * Entity class storing a selection of the data in a dataset.
 */
public class DataSlice {
    public static int DEFAULT_DEPTH_DIMENSION = 0;

    final public String dataFile;
    final public String dataPath;
    final public Class dataType;
    final public int dataRank;
    final public int[] dataShape;
    final public int sliceRank;
    final public long[] slicePos;
    final public int[] sliceShape;
    final public Object sliceData;
    final public boolean unsigned;

    //Empty Slice
    public DataSlice(String dataFile, String dataPath, int[] dataShape, Class dataType, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataShape.length;
        this.dataType = dataType;
        this.dataShape = dataShape;
        this.slicePos = new long[0];
        this.unsigned = unsigned;
        sliceRank = 0;
        sliceShape = new int[0];
        sliceData = null;
    }

    public DataSlice(String dataFile, String dataPath, int[] dataShape, Object sliceData, long[] slicePos, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataShape.length;
        this.dataShape = dataShape;
        this.slicePos = slicePos;
        this.sliceData = sliceData;
        this.unsigned = unsigned;
        if (isCompound()) {
            sliceShape = new int[]{Array.getLength(sliceData)};
            dataType = Object[].class;
        } else {
            sliceShape = Arr.getShape(sliceData);
            dataType = Arr.getComponentType(sliceData);
        }
        this.sliceRank = sliceShape.length;       
    }
    
    public DataSlice(String dataFile, String dataPath, int[] dataShape, Object sliceData, long[] slicePos,int[] slice_size, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataShape.length;
        this.dataShape = dataShape;
        this.slicePos = slicePos;
        this.sliceData = sliceData;
        this.unsigned = unsigned;
        this.sliceShape = slice_size;
        this.sliceRank = sliceShape.length;       
        this.dataType = Arr.getComponentType(sliceData);
    }    

    private static long[] getIndexArray(int[] dataShape, int index) {
        long[] indexArray = new long[dataShape.length];
        if (indexArray.length > 0) {
            indexArray[0] = index;
        }
        return indexArray;
    }

    public DataSlice(String dataFile, String dataPath, int[] dataShape, Object sliceData, int index, boolean unsigned) {
        this(dataFile, dataPath, dataShape, sliceData, getIndexArray(dataShape, index), unsigned);
    }

    public boolean isCompound() {
        return sliceData instanceof Object[][];
    }
    
      
    public int getNumberSlices(){
        return getNumberSlices(DEFAULT_DEPTH_DIMENSION);
    }

    public int getNumberSlices(int depth_dimension){
        return dataShape[depth_dimension];
    }
}
