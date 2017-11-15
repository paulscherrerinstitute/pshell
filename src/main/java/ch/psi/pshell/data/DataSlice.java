package ch.psi.pshell.data;

import ch.psi.pshell.core.Context;
import ch.psi.utils.Arr;
import java.lang.reflect.Array;

/**
 * Entity class storing a selection of the data in a dataset.
 */
public class DataSlice {

    final public String dataFile;
    final public String dataPath;
    final public Class dataType;
    final public int dataRank;
    final public int[] dataDimension;
    final public int sliceRank;
    final public long[] slicePos;
    final public int[] sliceSize;
    final public Object sliceData;
    final public boolean unsigned;

    //Empty Slice
    public DataSlice(String dataFile, String dataPath, int[] dataDimension, Class dataType, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataDimension.length;
        this.dataType = dataType;
        this.dataDimension = dataDimension;
        this.slicePos = new long[0];
        this.unsigned = unsigned;
        sliceRank = 0;
        sliceSize = new int[0];
        sliceData = null;
    }

    public DataSlice(String dataFile, String dataPath, int[] dataDimension, Object sliceData, long[] slicePos, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataDimension.length;
        this.dataDimension = dataDimension;
        this.slicePos = slicePos;
        this.sliceData = sliceData;
        this.unsigned = unsigned;
        if (isCompound()) {
            sliceSize = new int[]{Array.getLength(sliceData)};
            dataType = Object[].class;
        } else {
            sliceSize = Arr.getDimensions(sliceData);
            dataType = Arr.getComponentType(sliceData);
        }
        this.sliceRank = sliceSize.length;       
    }
    
    public DataSlice(String dataFile, String dataPath, int[] dataDimension, Object sliceData, long[] slicePos,int[] slice_size, boolean unsigned) {
        this.dataFile = dataFile;
        this.dataPath = dataPath;
        this.dataRank = dataDimension.length;
        this.dataDimension = dataDimension;
        this.slicePos = slicePos;
        this.sliceData = sliceData;
        this.unsigned = unsigned;
        this.sliceSize = slice_size;
        this.sliceRank = sliceSize.length;       
        this.dataType = Arr.getComponentType(sliceData);
    }    

    private static long[] getPagePos(int[] dataDimension, int page) {
        long[] pagePos = new long[dataDimension.length];
        if (pagePos.length > 0) {
            pagePos[0] = page;
        }
        return pagePos;
    }

    public DataSlice(String dataFile, String dataPath, int[] dataDimension, Object sliceData, int page, boolean unsigned) {
        this(dataFile, dataPath, dataDimension, sliceData, getPagePos(dataDimension, page), unsigned);
    }

    public boolean isCompound() {
        return ((sliceData != null) && (sliceData instanceof Object[][]));
    }
    
    public int getNumberSlices(){
        return dataDimension[Context.getInstance().getDataManager().getDepthDimension()];
    }
}
