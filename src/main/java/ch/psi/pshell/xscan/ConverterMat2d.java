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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConverterMat2d implements Converter {

    private static final Logger logger = Logger.getLogger(ConverterMat2d.class.getName());

    @Override
    public String getName() {
        return "Matlab 2D";
    }

    @Override
    public String getExtension() {
        return "mat";
    }
    
    @Override
    public boolean canConvert(DataSlice slice, Map<String, Object> info, Map<String, Object> attrs){
        try{
            String[] fieldNames = (String[]) info.get(Provider.INFO_FIELD_NAMES);     
            return true;
        } catch (Exception ex){
            return false;
        }
    }
    
    @Override
    public void convert(DataSlice slice, Map<String, Object> info, Map<String, Object> attrs, File output) throws Exception {
        List<List<List<Object>>> dlist = new ArrayList<>();
        List<Class<?>> clist = new ArrayList<>();
        int dsize = 0;
        int dcount = 0;
        List<Object> higherDimensionValues = new ArrayList<>();

        int[] shape = slice.sliceShape;
        Class type = slice.dataType;
        String[] fieldNames = (String[]) info.get(Provider.INFO_FIELD_NAMES);
        int[] dims = (int[]) info.get(ProviderFDA.INFO_FIELD_DIMENSIONS);

        int last = -1;
        int maxdim = 0;
        if (dims != null) {
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
        }
        if (maxdim != 1) {
            throw new RuntimeException("Converter only supports 2D data");
        }

        boolean changedHigerDimension = false;
        for (int i = 0; i < shape[0]; i++) {
            Object record = Array.get(slice.sliceData, i);
            int recordSize = Array.getLength(record);
            if (i == 0) {
                for (int j = 0; j < recordSize; j++) {
                    List<List<Object>> l = new ArrayList<List<Object>>();
                    l.add(new ArrayList<Object>());
                    dlist.add(l);
                    clist.add(Array.get(record, j).getClass());
                }
            } else {
                for (int j = 0; j < recordSize; j++) {
                    Object object = Array.get(record, j);
                    Object former = higherDimensionValues.get(j);
                    if ((former != null) && (!former.equals(object))) {
                        changedHigerDimension = true;
                    }
                }

            }
            if (changedHigerDimension) {
                if (dsize < dcount) {
                    dsize = dcount;
                }
                // Determine maximum dimension size
                // Add a new list for all component to the dlist
                for (List<List<Object>> lo : dlist) {
                    lo.add(new ArrayList<>());
                }
                dcount = 0;
                changedHigerDimension = false;
            }

            // Put data into data list
            for (int j = 0; j < recordSize; j++) {
                Object object = Array.get(record, j);
                List<List<Object>> lo = dlist.get(j);
                lo.get(lo.size() - 1).add(object); // add data to latest list
                if (higherDimensionValues.get(j) != null) {
                    higherDimensionValues.set(j, object);
                }
            }
            dcount++;
        }

        // Create Matlab vectors
        ArrayList<MLArray> matlablist = new ArrayList<>();
        for (int t = 0; t < dlist.size(); t++) {
            // Combine all lists to one big list (pad if there are data points missing)
            List<Object> list = new ArrayList<Object>();
            List<List<Object>> ol = dlist.get(t);

            for (List<Object> li : ol) {
                list.addAll(li);
                // Pad list if there are missing data points for some lines
                for (int i = li.size(); i < dsize; i++) {
                    list.add(Double.NaN);
                }
            }

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
