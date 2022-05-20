package ch.psi.pshell.xscan;

import ch.psi.pshell.data.Converter;
import ch.psi.pshell.data.DataSlice;
import ch.psi.pshell.data.Provider;
import ch.psi.pshell.data.ProviderFDA;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Serialize data received by a DataQueue into a Matlab file
 */
public class ConverterMat2dZigZag implements Converter {

    private static final Logger logger = Logger.getLogger(ConverterMat2dZigZag.class.getName());

    @Override
    public String getName() {
        return "Matlab 2D ZigZag";
    }

    @Override
    public String getExtension() {
        return "mat";
    }

    @Override
    public void convert(DataSlice slice, Map<String, Object> info, Map<String, Object> attrs, File output) throws Exception {
        List<List<Object>> dlist = new ArrayList<>();
        List<Class<?>> clist = new ArrayList<>();
        List<List<Object>> dlistTmp = new ArrayList<>();
        int delimiterCount = 0;
        boolean firstF = true;
        boolean firstC = true;

        int dsize = 0;
        int dcount = 0;
        List<Object> higherDimensionValues = new ArrayList<>();

        int[] shape = slice.sliceShape;
        Class type = slice.dataType;
        String[] fieldNames = (String[]) info.get(Provider.INFO_FIELD_NAMES);
        int[] dims = (int[]) info.get(ProviderFDA.INFO_FIELD_DIMENSIONS);

        int last = -1;
        int maxdim = 0;
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] > maxdim) {
                maxdim = dims[i];
            }
            if ((dims[i] > 0) && (dims[i] != last)) {
                higherDimensionValues.add(new Object());
            } else {
                higherDimensionValues.add(null);
            }
            last = dims[i];
        }

        if (maxdim != 1) {
            throw new RuntimeException("Serializer does only support 2D data (" + (maxdim + 1) + "D data found)");
        }

        boolean changedHigerDimension = false;
        for (int i = 0; i < shape[0]; i++) {
            Object record = Array.get(slice.sliceData, i);
            int recordSize = Array.getLength(record);

            if (firstC) {
                for (int j = 0; j < recordSize; j++) {
                    dlist.add(new ArrayList<Object>());
                    clist.add(Array.get(record, j).getClass());
                    firstC = false;
                }
            }

            // Initialize list
            if (firstF) {
                for (int j = 0; j < recordSize; j++) {
                    dlistTmp.add(new ArrayList<Object>());
                }
                firstF = false;
            }

            // Put data into data list
            for (int j = 0; j < recordSize; j++) {
                Object object = Array.get(record, j);
                dlistTmp.get(j).add(object);
                Object former = higherDimensionValues.get(j);
                if (former != null) {
                    if (!former.equals(object)) {
                        changedHigerDimension = true;
                    }
                    higherDimensionValues.set(j, object);
                }
            }

            if (changedHigerDimension || (i == (shape[0] - 1))) {
                if (dsize < dcount) {
                    dsize = dcount;
                }
                dcount = 0;

                // Add temporary list to final list
                for (int j = 0; j < dlist.size(); j++) {
                    if (delimiterCount % 2 == 1) {
                        Collections.reverse(dlistTmp.get(j));
                    }
                    dlist.get(j).addAll(dlistTmp.get(j));
                }

                dlistTmp.clear();

                firstF = true;
                delimiterCount++;
                changedHigerDimension = false;
            }

            dcount++;
        }

        logger.info("dsize: " + dsize);
        // Create Matlab vectors
        ArrayList<MLArray> matlablist = new ArrayList<MLArray>();
        logger.info("dlist size: " + dlist.size());
        for (int t = 0; t < dlist.size(); t++) {
            List<Object> list = dlist.get(t);

            if (clist.get(t).isArray()) {
                // Array Handling
            } else if (clist.get(t).equals(Double.class)) {
                // Data is of type Double
                MLDouble darray = new MLDouble(ConverterMat.escapeString(fieldNames[t]), (Double[]) list.toArray(new Double[list.size()]), dsize);
                matlablist.add(darray);
            }

        }

        // Write Matlab file
        MatFileWriter writerr = new MatFileWriter();
        writerr.write(output, matlablist);
    }
}
