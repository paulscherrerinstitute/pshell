package ch.psi.pshell.data;

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
    final public int[] slicePos;
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
        this.slicePos = new int[0];
        this.unsigned = unsigned;
        sliceRank = 0;
        sliceSize = new int[0];
        sliceData = null;
    }

    public DataSlice(String dataFile, String dataPath, int[] dataDimension, Object sliceData, int[] slicePos, boolean unsigned) {
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
        sliceRank = sliceSize.length;
    }

    private static int[] getPagePos(int[] dataDimension, int page) {
        int[] pagePos = new int[dataDimension.length];
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
}
