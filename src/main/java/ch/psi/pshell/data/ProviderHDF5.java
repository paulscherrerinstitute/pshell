package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import ch.psi.utils.Str;
import ch.systemsx.cisd.base.mdarray.MDAbstractArray;
import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDDoubleArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDIntArray;
import ch.systemsx.cisd.base.mdarray.MDLongArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5CompoundType;
import ch.systemsx.cisd.hdf5.HDF5DataClass;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures.HDF5FloatStorageFeatureBuilder;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5GenericStorageFeatures.HDF5GenericStorageFeatureBuilder;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures.HDF5IntStorageFeatureBuilder;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.HDF5ObjectInformation;
import ch.systemsx.cisd.hdf5.HDF5ObjectType;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provider implementation storing data in HDF5 files.
 */
public class ProviderHDF5 implements Provider {

    IHDF5Writer writer;
    File writerFile;
    final HashMap<String, HDF5CompoundType<Object[]>> compoundTypes;

    public ProviderHDF5() {
        compoundTypes = new HashMap<>();
    }

    @Override
    public String getFileType() {
        return "h5";
    }

    @Override
    public boolean isPacked() {
        return true;
    }

    @Override
    public void openOutput(File root) {
        writerFile = root;
        if (writerFile != null) {
            writerFile.mkdirs();
            writerFile.delete();
            writer = HDF5Factory.open(writerFile.getPath());    
        }
    }

    @Override
    public void closeOutput() {
        try {
            if (writer != null) {          
                try {
                    writer.close();
                } catch (Exception ex) {
                    Logger.getLogger(ProviderHDF5.class.getName()).log(Level.WARNING, null, ex);
                }
            }
            synchronized (compoundTypes) {
                compoundTypes.clear();
            }
        } finally {
            writerFile = null;
            writer = null;
        }

    }
    
    void assertOpenOutput() throws IllegalStateException {
        if (writer == null) {
            throw new IllegalStateException("HDF5 writer not opened");
        }
    }

    @Override
    public void flush() {
        writer.file().flush();
    }

    //Data reading    
    IHDF5Reader openInputFile(String file) {
        if ((writer != null) && (writerFile != null) && IO.isSubPath(file, writerFile.getPath())) {
            return writer;
        }
        if (!file.endsWith(getFileType())) {
            file += "." + getFileType();
        }
        return HDF5Factory.openForReading(file);
    }

    Object[] getGroupStructure(IHDF5Reader reader, String group) {
        List<String> elements = reader.getGroupMembers(group);
        List contents = new ArrayList();
        List subgroups = new ArrayList();
        HDF5LinkInformation info = reader.object().getLinkInformation(group);
        contents.add(info.getName());
        for (String element : elements) {
            info = reader.object().getLinkInformation(group + "/" + element);
            if (info.isGroup()) {
                subgroups.add(getGroupStructure(reader, info.getPath()));
            } else {
                contents.add(info.getName());
            }
        }
        contents.addAll(subgroups);
        return contents.toArray();
    }

    @Override
    public Object[] getStructure(String root) {
        Object[] ret;
        IHDF5Reader reader = openInputFile(root);
        try {
            ret = getGroupStructure(reader, "/");
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
        return ret;
    }

    @Override
    public String[] getChildren(String root, String path) throws IOException {
        IHDF5Reader reader = openInputFile(root);
        ArrayList<String> ret = new ArrayList<>();
        try {
            if (!path.endsWith("/")) {
                path += "/";
            }
            for (String child : reader.getGroupMembers(path)) {
                ret.add(path + child);
            }
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isDataset(String root, String path) {
        IHDF5Reader reader = openInputFile(root);
        try {
            HDF5LinkInformation info = reader.object().getLinkInformation(path);
            return info.isDataSet();
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }

    }

    @Override
    public boolean isGroup(String root, String path) {
        IHDF5Reader reader = openInputFile(root);
        try {
            HDF5LinkInformation info = reader.object().getLinkInformation(path);
            return info.isGroup();
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
    }
    
    public DataSlice getData(String root, String path,  long[] index, int[] shape) throws IOException {        
        IHDF5Reader reader = openInputFile(root);
        try {
            HDF5LinkInformation info = reader.object().getLinkInformation(path);
            if (!info.isDataSet()) {
                return null;
            }
            HDF5DataSetInformation dsinfo = reader.object().getDataSetInformation(path);
            boolean unsigned = !dsinfo.isSigned();
            int rank = dsinfo.getRank();
            long[] dims = dsinfo.getDimensions();
            int[] idims = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                idims[i] = (int) dims[i];
            }

            //Assume will read it all
            Object array = null;

            switch (dsinfo.getTypeInformation().getDataClass()) {
                case FLOAT:
                    switch (dsinfo.getTypeInformation().getElementSize()) {
                        case 4:                                    
                            array = reader.float32().readMDArrayBlock(path, shape, index).getAsFlatArray();
                            break;
                        default:
                            array = reader.float64().readMDArrayBlock(path, shape, index).getAsFlatArray();
                    }
                    break;
                case INTEGER:
                    switch (dsinfo.getTypeInformation().getElementSize()) {
                        case 1:
                            array =  unsigned ? reader.uint8().readMDArrayBlock(path, shape, index).getAsFlatArray()
                                              : reader.int8().readMDArrayBlock(path, shape, index).getAsFlatArray();
                            break;
                        case 2:
                            array =  unsigned ? reader.uint16().readMDArrayBlock(path, shape, index).getAsFlatArray()
                                              : reader.int16().readMDArrayBlock(path, shape, index).getAsFlatArray();
                            break;
                        case 8:
                            array =  unsigned ? reader.uint64().readMDArrayBlock(path, shape, index).getAsFlatArray()
                                              : reader.int64().readMDArrayBlock(path, shape, index).getAsFlatArray();
                            break;
                        default:
                            array =  unsigned ? reader.uint32().readMDArrayBlock(path, shape, index).getAsFlatArray()
                                              : reader.int32().readMDArrayBlock(path, shape, index).getAsFlatArray();
                    }
                    break;
                case ENUM:
                case STRING:
                case BOOLEAN:
                case BITFIELD:
                case COMPOUND:
                default:
                    break;
            }
            if (array != null) {
                return new  DataSlice(root, path, idims, array, index, shape, unsigned);
            }
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
        return null;
    }

    @Override
    public DataSlice getData(String root, String path, int index) throws IOException {
        IHDF5Reader reader = openInputFile(root);
        DataSlice ret = null;
        try {
            HDF5LinkInformation info = reader.object().getLinkInformation(path);
            if (!info.isDataSet()) {
                return null;
            }
            HDF5DataSetInformation dsinfo = reader.object().getDataSetInformation(path);
            boolean unsigned = !dsinfo.isSigned();
            int rank = dsinfo.getRank();
            long[] dims = dsinfo.getDimensions();
            int[] idims = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                idims[i] = (int) dims[i];
            }

            //Assume will read it all
            Object array = null;

            switch (dsinfo.getTypeInformation().getDataClass()) {
                case ENUM:
                case STRING:
                    switch (rank) {
                        case 0:
                            array = reader.readString(path);
                            break;
                        default:
                            try{
                                array = reader.readStringArray(path);
                            } catch (NullPointerException ex){
                                //This is a bug in JHDF5, empty dataset is rising an exception instead of returning empty aray
                                array = new String[0];
                            }
                    }
                    break;
                case FLOAT:
                    switch (rank) {
                        case 0:
                            array = reader.readDouble(path);
                            break;
                        case 1:
                            array = reader.readDoubleArray(path);
                            break;
                        case 2:
                            array = reader.readDoubleMatrix(path);
                            break;
                        case 3:
                            switch (dsinfo.getTypeInformation().getElementSize()) {
                                case 4:                                    
                                    array = getMatrixArray (reader.float32().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                                    break;
                                default:
                                    array = getMatrixArray (reader.float64().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                            }
                            break;
                    }
                    break;
                case INTEGER:
                    switch (rank) {
                        case 0:
                            switch (dsinfo.getTypeInformation().getElementSize()) {
                                case 1:
                                    array = unsigned ? reader.uint8().read(path) : reader.int8().read(path);
                                    break;
                                case 2:
                                    array = unsigned ? reader.uint16().read(path) : reader.int16().read(path);
                                    break;
                                case 8:
                                    array = unsigned ? reader.uint64().read(path) : reader.int64().read(path);
                                    break;
                                default:
                                    array = unsigned ? reader.uint32().read(path) : reader.int32().read(path);
                            }
                            break;
                        case 1:
                            switch (dsinfo.getTypeInformation().getElementSize()) {
                                case 1:
                                    array = unsigned ? reader.uint8().readArray(path) : reader.int8().readArray(path);
                                    break;
                                case 2:
                                    array = unsigned ? reader.uint16().readArray(path) : reader.int16().readArray(path);
                                    break;
                                case 8:
                                    array = unsigned ? reader.uint64().readArray(path) : reader.int64().readArray(path);
                                    break;
                                default:
                                    array = unsigned ? reader.uint32().readArray(path) : reader.int32().readArray(path);
                            }
                            break;
                        case 2:
                            switch (dsinfo.getTypeInformation().getElementSize()) {
                                case 1:
                                    array = unsigned ? reader.uint8().readMatrix(path) : reader.int8().readMatrix(path);
                                    break;
                                case 2:
                                    array = unsigned ? reader.uint16().readMatrix(path) : reader.int16().readMatrix(path);
                                    break;
                                case 8:
                                    array = unsigned ? reader.uint64().readMatrix(path) : reader.int64().readMatrix(path);
                                    break;
                                default:
                                    array = unsigned ? reader.uint32().readMatrix(path) : reader.int32().readMatrix(path);
                            }
                            break;
                        case 3:
                            switch (dsinfo.getTypeInformation().getElementSize()) {
                                case 1:
                                    array =  getMatrixArray (unsigned ? reader.uint8().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)) 
                                                                      : reader.int8().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                                    break;
                                case 2:
                                    array =  getMatrixArray (unsigned ? reader.uint16().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)) 
                                                                      : reader.int16().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                                    break;
                                case 8:
                                    array =  getMatrixArray (unsigned ? reader.uint64().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)) 
                                                                      : reader.int64().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                                    break;
                                default:
                                    array =  getMatrixArray (unsigned ? reader.uint32().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)) 
                                                                      : reader.int32().readMDArrayBlock(path, getMatrixShape(null), getMatrixOffset(index)));
                            }
                            break;
                    }
                    break;
                case BOOLEAN:
                    switch (rank) {
                        case 0:
                            array = reader.readBoolean(path);
                            break;
                        case 1:
                            byte[] arr = reader.uint8().readArray(path);
                            array = Convert.fromByteArray(arr, boolean.class);
                            break;
                        case 2:
                            byte[][] matrix = reader.uint8().readMatrix(path);
                            boolean[][] bmatrix = new boolean[matrix.length][];
                            for (int i = 0; i < matrix.length; i++) {
                                bmatrix[i]= (boolean[]) Convert.fromByteArray(matrix[i], boolean.class);;
                            }
                            array = bmatrix;
                            break;
                    }
                    break;
                case BITFIELD:
                    /*
                    BitSet b = reader.bool().readBitField(path);
                    array = new boolean[b.size()];
                    for (int i =0; i< b.size(); i++ ){
                        ((boolean[])array)[i] = b.get(i);
                    }
                    break;                    
                     */

                    BitSet[] b = reader.bool().readBitFieldArray(path);
                    array = new boolean[b.length];
                    for (int i = 0; i < b.length; i++) {
                        ((boolean[]) array)[i] = b[i].get(0);
                    }
                    break;
                case COMPOUND:
                    HDF5CompoundType<Object[]> compoundType = getCompoundType(reader, path);
                    array = reader.compound().readArray(path, compoundType);
                    break;
            }
            if (array != null) {
                ret = new DataSlice(root, path, idims, array, index, unsigned);
            } else {
                ret = new DataSlice(root, path, idims, null, unsigned);
            }
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
        return ret;
    }

    @Override
    public Map<String, Object> getInfo(String root, String path) {
        HashMap<String, Object> ret = new HashMap<>();
        IHDF5Reader reader = openInputFile(root);
        try {
            HDF5LinkInformation info = reader.object().getLinkInformation(path);
            HDF5ObjectType type = info.getType();
            ret.put("Type", type);
            //ret.put("Size", reader.object().getSize(path));

            if (info.isDataSet()) {
                HDF5ObjectInformation objinfo = reader.object().getObjectInformation(path);
                ret.put("Creation", Chrono.getTimeStr(objinfo.getCreationTime() * 1000, "dd/MM/YY HH:mm:ss"));

                HDF5DataSetInformation dsinfo = reader.object().getDataSetInformation(path);

                long[] dims = dsinfo.getDimensions();
                int[] idims = new int[dims.length];
                for (int i = 0; i < dims.length; i++) {
                    idims[i] = (int) dims[i];
                }
                ret.put("Dimensions", idims);
                ret.put("Elements", dsinfo.getNumberOfElements());
                ret.put("Element Size", dsinfo.getTypeInformation().getElementSize());
                ret.put("Rank", dsinfo.getRank());
                ret.put("Size", dsinfo.getSize());
                ret.put("Layout", dsinfo.getStorageLayout().name());
                ret.put("Data Type", dsinfo.getTypeInformation().getDataClass().toString());
                ret.put("Signed", dsinfo.getTypeInformation().isSigned());
                int[] chunk_sizes = dsinfo.tryGetChunkSizes();
                if ((chunk_sizes!=null) && (chunk_sizes.length>0)){
                    ret.put("Chunk Sizes", chunk_sizes);
                }

                if (dsinfo.getTypeInformation().getDataClass() == HDF5DataClass.COMPOUND) {
                    for (String compoundDataInfo : COMPOUND_DATA_INFO) {
                        Object attr = getAttribute(reader, path, compoundDataInfo);
                        if (attr != null) {
                            ret.put(compoundDataInfo, attr);
                        }
                    }
                }

            } else if (info.isGroup()) {
            }
            return ret;
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
    }

    @Override
    public Map<String, Object> getAttributes(String root, String path) {
        IHDF5Reader reader = openInputFile(root);
        try {
            return getAttributes(reader, path);
        } finally {
            if ((reader != null) && (reader != writer)) {
                reader.close();
            }
        }
    }

    Map<String, Object> getAttributes(IHDF5Reader reader, String path) {
        HashMap<String, Object> ret = new HashMap<>();
        List<String> attrs = reader.object().getAllAttributeNames(path);
        for (String attr : attrs) {
            if (!Arr.containsEqual(COMPOUND_DATA_INFO, attr)) {
                try {
                    Object val = getAttribute(reader, path, attr);
                    ret.put(attr, val);
                } catch (Exception ex) {
                    ret.put("Exception", ex.getMessage());
                }
            }
        }
        return ret;

    }

    Object getAttribute(IHDF5Reader reader, String path, String attr) {
        Object val;
        HDF5DataTypeInformation info = reader.object().getAttributeInformation(path, attr);
        boolean unsigned = !info.isSigned();
        if (info.isArrayType()) {
            switch (info.getDataClass()) {
                case INTEGER:
                    switch (info.getElementSize()) {
                        case 1:
                            val = unsigned ? reader.uint8().getArrayAttr(path, attr) : reader.int8().getArrayAttr(path, attr);
                            break;
                        case 2:
                            val = unsigned ? reader.uint16().getArrayAttr(path, attr) : reader.int16().getArrayAttr(path, attr);
                            break;
                        case 8:
                            val = unsigned ? reader.uint64().getArrayAttr(path, attr) : reader.int64().getArrayAttr(path, attr);
                            break;
                        default:
                            val = unsigned ? reader.uint32().getArrayAttr(path, attr) : reader.int32().getArrayAttr(path, attr);
                            break;
                    }
                    break;
                case FLOAT:
                    switch (info.getElementSize()) {
                        case 4:
                            val = reader.float32().getArrayAttr(path, attr);
                            break;
                        default:
                            val = reader.float64().getArrayAttr(path, attr);
                            break;
                    }
                    break;
                default:
                    val = reader.string().getArrayAttr(path, attr);
            }
        } else {
            switch (info.getDataClass()) {
                case INTEGER:
                    switch (info.getElementSize()) {
                        case 1:
                            val = unsigned ? reader.uint8().getAttr(path, attr) : reader.int8().getAttr(path, attr);
                            break;
                        case 2:
                            val = unsigned ? reader.uint16().getAttr(path, attr) : reader.int16().getAttr(path, attr);
                            break;
                        case 4:
                            val = unsigned ? reader.uint32().getAttr(path, attr) : reader.int32().getAttr(path, attr);
                            break;
                        default:
                            val = unsigned ? reader.uint64().getAttr(path, attr) : reader.int64().getAttr(path, attr);
                            break;
                    }
                    break;
                case FLOAT:
                    switch (info.getElementSize()) {
                        case 4:
                            val = reader.float32().getAttr(path, attr);
                            break;
                        default:
                            val = reader.float64().getAttr(path, attr);
                            break;
                    }
                    break;
                case BOOLEAN:
                    val = reader.bool().getAttr(path, attr);
                    break;
                default:
                    val = reader.string().getAttr(path, attr);
            }
        }
        return val;
    }

    //Data writing
    @Override
    public void createGroup(String path) {
        assertOpenOutput();
        try{
            writer.object().createGroup(path);
        } catch (Exception ex){            
        }
    }

    @Override
    public void setDataset(String path, Object data, Class type, int rank, int[] dims, boolean unsigned, Map features) {
        assertOpenOutput();
        HDF5GenericStorageFeatures sf = getStorageFeatures(features, dims);
        if (rank>0){
            if (Number.class.isAssignableFrom(type)){
                data = Convert.toPrimitiveArray(data, (type == BigInteger.class) ? long.class : Convert.getPrimitiveClass(type));
            }
        }
        
        if (rank == 0) {
            if (type == Double.class) {
                writer.float64().write(path, (double) data);
            } else if (type == Float.class) {
                writer.float32().write(path, (float) data);
            } else if (type == BigInteger.class) {
                if (unsigned) {
                    writer.uint64().write(path,((BigInteger) data).longValue());
                } else {
                    writer.int64().write(path, ((BigInteger) data).longValue());
                }
            } else if (type == Long.class) {
                if (unsigned) {
                    writer.uint64().write(path, (long) data);
                } else {
                    writer.int64().write(path, (long) data);
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().write(path, (int) data);
                } else {
                    writer.int32().write(path, (int) data);
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().write(path, (short) data);
                } else {
                    writer.int16().write(path, (short) data);
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.uint8().write(path, (byte) data);
                } else {
                    writer.int8().write(path, (byte) data);
                }
            } else if (type == String.class) {
                writer.string().write(path, (String) data, sf);
            } else if (type == Boolean.class) {
                writer.bool().write(path, (Boolean) data);
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        } else if (rank == 1) {
            if (type == Double.class) {
                writer.float64().writeArray(path, (double[]) data, toFloatFeatures(sf));
            } else if (type == Float.class) {
                writer.float32().writeArray(path, (float[]) data, toFloatFeatures(sf));
            } else if ((type == Long.class) || (type == BigInteger.class)) {
                if (unsigned) {
                    writer.uint64().writeArray(path, (long[]) data, toIntFeatures(sf));
                } else {
                    writer.int64().writeArray(path, (long[]) data, toIntFeatures(sf));
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().writeArray(path, (int[]) data, toIntFeatures(sf));
                } else {
                    writer.int32().writeArray(path, (int[]) data, toIntFeatures(sf));
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().writeArray(path, (short[]) data, toIntFeatures(sf));
                } else {
                    writer.int16().writeArray(path, (short[]) data, toIntFeatures(sf));
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.uint8().writeArray(path, (byte[]) data, toIntFeatures(sf));
                } else {
                    writer.int8().writeArray(path, (byte[]) data, toIntFeatures(sf));
                }
            } else if (type == String.class) {
                writer.string().writeArray(path, (String[]) data, sf);
            } else if (type == Boolean.class) {
                BitSet[] bs = new BitSet[Array.getLength(data)];
                for (int i = 0; i < Array.getLength(data); i++) {
                    bs[i] = new BitSet(1);
                    bs[i].set(0, ((boolean[]) data)[i]);
                }
                writer.bool().writeBitFieldArray(path, bs, toIntFeatures(sf));
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        } else if (rank == 2) {
            if (type == Double.class) {
                writer.float64().writeMatrix(path, (double[][]) data, toFloatFeatures(sf));
            } else if (type == Float.class) {
                writer.float32().writeMatrix(path, (float[][]) data, toFloatFeatures(sf));
            } else if ((type == Long.class) || (type == BigInteger.class)) {              
                if (unsigned) {
                    writer.uint64().writeMatrix(path, (long[][]) data, toIntFeatures(sf));
                } else {
                    writer.int64().writeMatrix(path, (long[][]) data, toIntFeatures(sf));
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().writeMatrix(path, (int[][]) data, toIntFeatures(sf));
                } else {
                    writer.int32().writeMatrix(path, (int[][]) data, toIntFeatures(sf));
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().writeMatrix(path, (short[][]) data, toIntFeatures(sf));
                } else {
                    writer.int16().writeMatrix(path, (short[][]) data, toIntFeatures(sf));
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.uint8().writeMatrix(path, (byte[][]) data, toIntFeatures(sf));
                } else {
                    writer.int8().writeMatrix(path, (byte[][]) data, toIntFeatures(sf));
                }
            } else if (type == Boolean.class) {
                BitSet[] bs = new BitSet[Array.getLength(data)];
                for (int i = 0; i < Array.getLength(data); i++) {
                    bs[i] = new BitSet(1);
                    for (int j = 0; j < (((boolean[][]) data)[i]).length; j++) {
                        bs[i].set(j, ((boolean[][]) data)[i][j]);
                    }
                }
                writer.bool().writeBitFieldArray(path, bs, toIntFeatures(sf));
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        } else if (rank == 3) {
            if (type == Double.class) {
                double[][][] d = (double[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {                    
                    MDDoubleArray array = new MDDoubleArray((double[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    writer.float64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                }
                
            } else if (type == Float.class) {
                float[][][] d = (float[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {
                    MDFloatArray array = new MDFloatArray((float[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    writer.float32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                }
            } else if ((type == Long.class) || (type == BigInteger.class)) {               
                long[][][] d = (long[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {
                    MDLongArray array = new MDLongArray((long[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    if (unsigned) {
                        writer.uint64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    } else {
                        writer.int64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    }
                }
            } else if (type == Integer.class) {
                int[][][] d = (int[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {
                    MDIntArray array = new MDIntArray((int[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    if (unsigned) {
                        writer.uint32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    } else {
                        writer.int32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    }
                }
            } else if (type == Short.class) {
                short[][][] d = (short[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {
                    MDShortArray array = new MDShortArray((short[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    if (unsigned) {
                        writer.uint16().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    } else {
                        writer.int16().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    }
                }
            } else if (type == Byte.class) {
                byte[][][] d = (byte[][][]) data;
                createDataset(path, type, get3dMatrixDims(d), unsigned, features);
                for (int i = 0; i < d.length; i++) {
                    MDByteArray array = new MDByteArray((byte[])Convert.flatten(d[i]), getMatrixShape(d[i]));
                    if (unsigned) {
                        writer.uint8().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    } else {
                        writer.int8().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(i));
                    }
                }
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        } else {
            throw new UnsupportedOperationException("Not supported rank = " + rank);
        }
    }

    @Override
    public void createDataset(String path, Class type, int[] dimensions, boolean unsigned, Map features) {
        assertOpenOutput();
        HDF5GenericStorageFeatures sf = getStorageFeatures(features, dimensions);
        int[] cs = getChunkSize(features, dimensions);
        if(cs == null) {
            if (type == Double.class) {
                writer.float64().createMDArray(path, dimensions, toFloatFeatures(sf));
            } else if (type == Float.class) {
                writer.float32().createMDArray(path, dimensions, toFloatFeatures(sf));
            } else if ((type == Long.class) || (type==BigInteger.class)) {
                if (unsigned) {
                    writer.uint64().createMDArray(path, dimensions, toIntFeatures(sf));
                } else {
                    writer.int64().createMDArray(path, dimensions, toIntFeatures(sf));
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().createMDArray(path, dimensions, toIntFeatures(sf));
                } else {
                    writer.int32().createMDArray(path, dimensions, toIntFeatures(sf));
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().createMDArray(path, dimensions, toIntFeatures(sf));
                } else {
                    writer.int16().createMDArray(path, dimensions, toIntFeatures(sf));
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.uint8().createMDArray(path, dimensions, toIntFeatures(sf));
                } else {
                    writer.int8().createMDArray(path, dimensions, toIntFeatures(sf));
                }
            } else if ((type == String.class) && (dimensions.length == 1)) {
                writer.string().createArrayVL(path, 0, 0, sf);
            } else if ((type == Boolean.class) && (dimensions.length <= 2)) {
                writer.bool().createBitFieldArray(path, 1, dimensions[0], toIntFeatures(sf));
            } else {
                throw new UnsupportedOperationException("Not supported type: " + type);
            }
        } else {
            if (type == Double.class) {
                writer.float64().createMDArray(path, MDArray.toLong(dimensions), cs, toFloatFeatures(sf));
            } else if (type == Float.class) {
                writer.float32().createMDArray(path, MDArray.toLong(dimensions), cs,toFloatFeatures(sf));
            } else if ((type == Long.class) || (type==BigInteger.class)) {
                if (unsigned) {
                    writer.uint64().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                } else {
                    writer.int64().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                } else {
                    writer.int32().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                } else {
                    writer.int16().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.uint8().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                } else {
                    writer.int8().createMDArray(path, MDArray.toLong(dimensions), cs, toIntFeatures(sf));
                }
            } else {
                throw new UnsupportedOperationException("Not supported type: " + type + " - chunk size:" + Str.toString(cs));
            }
            
        }
    }
    
    byte getDeflation(Map features){
        Object compression = features.get("compression");
        if (compression != null){
            if (compression instanceof Number){
                return ((Number)compression).byteValue();
            }
            
            if ("true".equalsIgnoreCase(String.valueOf(compression)) || "default".equalsIgnoreCase(String.valueOf(compression))){
                return HDF5GenericStorageFeatures.DEFAULT_DEFLATION_LEVEL;
            }
            if ("max".equalsIgnoreCase(String.valueOf(compression))){
                return HDF5GenericStorageFeatures.MAX_DEFLATION_LEVEL;
            }            
        }            
        return 0;
    }
    
    HDF5GenericStorageFeatures getStorageFeatures(Map features, int[] dimensions){
        if(features!=null){
            
            Object layout = features.get("layout");
            Byte deflation = getDeflation(features);
            Boolean shuffle = "true".equalsIgnoreCase(String.valueOf(features.get("shuffle")));
            
            if (deflation>0){
                if (shuffle){
                    HDF5GenericStorageFeatureBuilder builder = new HDF5GenericStorageFeatureBuilder();
                    return (HDF5GenericStorageFeatures) builder.deflateLevel(deflation).shuffleBeforeDeflate().features();                    
                }else {
                    return HDF5GenericStorageFeatures.createDeflation(deflation);
                }
            }
            
            if (layout != null){
                switch (String.valueOf(layout)){
                    case "compact":
                        return HDF5GenericStorageFeatures.GENERIC_COMPACT; 
                    case "contiguous":
                        return HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS;        
                    case "chunked":
                        return HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION;
                }
            }            
        }
        
        if ((dimensions!=null) && (dimensions.length>0)){
            boolean fixedSize = true;
            for (int x : dimensions){
                if (x==0){
                    fixedSize = false;
                }
            }
            if (fixedSize){
                return HDF5GenericStorageFeatures.GENERIC_CONTIGUOUS;
            }
        }
        return HDF5GenericStorageFeatures.GENERIC_NO_COMPRESSION;
    }
    
    HDF5FloatStorageFeatures toFloatFeatures(HDF5GenericStorageFeatures sf){
        //TODO: Workaroung JHDF5 bug
        if ((sf.isShuffleBeforeDeflate()) && (sf.isDeflating())){
            HDF5FloatStorageFeatureBuilder builder = new HDF5FloatStorageFeatureBuilder();
            return (HDF5FloatStorageFeatures) builder.deflateLevel(sf.getDeflateLevel()).shuffleBeforeDeflate().features();                
        }
        return HDF5FloatStorageFeatures.createFromGeneric(sf);
    }

    HDF5IntStorageFeatures toIntFeatures(HDF5GenericStorageFeatures sf){
        //TODO: Workaroung JHDF5 bug
        if ((sf.isShuffleBeforeDeflate()) && (sf.isDeflating())){
            HDF5IntStorageFeatureBuilder builder = new HDF5IntStorageFeatureBuilder();
            return (HDF5IntStorageFeatures) builder.deflateLevel(sf.getDeflateLevel()).shuffleBeforeDeflate().features();                
        }      
        return HDF5IntStorageFeatures.createFromGeneric(sf);
    }
    
    int[] getChunkSize(Map features, int[] dimensions){
        if (features!=null){
            if (features.containsKey("chunk")){
                try{
                    List l = (List) features.get("chunk");
                    return (int[])Convert.doubleToInt((double[]) Convert.toDouble(Convert.toArray(l)));
                } catch (Exception ex) {
                }
            }  
            //Fix to JHDF5 setting chunk size to [1,1,1] for variable size datasets
            if ((getDeflation(features) > 0) && (dimensions!=null)){
                int[] ret = new int[dimensions.length];
                for(int i=0; i<dimensions.length; i++){
                    ret[i] = (dimensions[i]==0) ? 1 :dimensions[i];
                }
                return ret;
            } 
        }
        return null;
    }

    //TODO: find way to read compound descriptor directly from the file and not through these attributes.
    //      If solved compoundTypes dictionaire could be removed.
    HDF5CompoundType<Object[]> getCompoundType(IHDF5Reader reader, String path) throws IOException {
        if (reader == writer) {
            synchronized (compoundTypes) {
                HDF5CompoundType<Object[]> compoundType = compoundTypes.get(path);
                if (compoundType == null) {
                    throw new IllegalStateException("No compound type defined for path: " + path);
                }
                return compoundType;
            }
        }

        try {
            String[] names = (String[]) getAttribute(reader, path, INFO_FIELD_NAMES);
            String[] typeNames = (String[]) getAttribute(reader, path, INFO_FIELD_TYPES);
            int[] lengths = (int[]) getAttribute(reader, path, INFO_FIELD_LENGTHS);
            Class[] types = new Class[typeNames.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = Class.forName(typeNames[i]);
            }
            //HDF5CompoundType<Object[]> compoundType = reader.compound().getInferredType(path, new String[]{"inp","sin","out","arr"}, new Object[]{0.0,0.0,0.0,new double[]{1,2,3,4,5,6,7,8,9,0}});                                        //Object[] template =  createCompoundTemplate(path, types, lengths);     
            Object[] template = createCompoundTemplate(path, types, lengths);
            HDF5CompoundType<Object[]> compoundType = reader.compound().getInferredType(path, names, template);
            return compoundType;
        } catch (Exception ex) {
            throw new IOException("Invalid compound descriptor attributes");
        }

    }

    Object[] createCompoundTemplate(String path, Class[] types, int[] lengths) throws IOException {
        Object[] ret = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].isArray()) {
                ret[i] = java.lang.reflect.Array.newInstance(types[i].getComponentType(), lengths[i]);
            } else if (Number.class.isAssignableFrom(types[i])) {
                try {
                    Constructor c = types[i].getConstructor(String.class);
                    ret[i] = c.newInstance("0");
                } catch (Exception ex) {
                    throw new IOException(ex.getMessage());
                }
            } else {
                ret[i] = "";
            }
        }
        return ret;
    }

    @Override
    public void createDataset(String path, String[] names, Class[] types, int[] lengths, Map features) throws IOException {
        assertOpenOutput();
        Object[] template = createCompoundTemplate(path, types, lengths);
        HDF5GenericStorageFeatures sf = getStorageFeatures(features, null);
        synchronized (compoundTypes) {
            try {
                HDF5CompoundType<Object[]> compoundType = writer.compound().getInferredType(path, names, template);
                compoundTypes.put(path, compoundType);
                writer.compound().createArray(path, compoundType, 0, sf);
            } catch (Exception ex) {
                throw new IOException("Invalid types in table dataset: " + Convert.arrayToString(types, ", "));
            }
        }

        String[] typesNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typesNames[i] = types[i].getName();
        }
        setAttribute(path, INFO_FIELD_NAMES, names, String[].class, false);
        setAttribute(path, INFO_FIELD_TYPES, typesNames, String[].class, false);
        setAttribute(path, INFO_FIELD_LENGTHS, lengths, int[].class, false);
        setAttribute(path, INFO_FIELDS, names.length, Integer.class, false);
    }

    @Override
    public void setItem(String path, Object data, Class type, int index) {       
        assertOpenOutput();
        if (type == null) {
            //For float types don't leave default '0' when writing null: set NaN instead.
            HDF5DataSetInformation info = writer.object().getDataSetInformation(path);
            int rank = info.getRank();
            switch (info.getTypeInformation().getDataClass()) {
                case FLOAT:
                    if (info.getTypeInformation().getElementSize() == 4) {
                        if (rank < 2) {
                            type = Float.class;
                            data = Float.NaN;
                        } else if (rank == 2) {
                            if (info.getDimensions()[1] > 0) {
                                type = float[].class;
                                data = new float[(int) info.getDimensions()[1]];
                                Arrays.fill((float[]) data, Float.NaN);
                            }
                        } else if (rank == 3) {
                            if ((info.getDimensions()[0] > 0) && (info.getDimensions()[1] > 0)) {
                                type = float[][].class;
                                data = new float[(int) info.getDimensions()[0]][(int) info.getDimensions()[1]];
                                for (int i = 0; i < ((float[][]) data).length; i++) {
                                    Arrays.fill(((float[][]) data)[i], Float.NaN);
                                }
                            }
                        }
                    } else {
                        if (rank < 2) {
                            type = Double.class;
                            data = Double.NaN;
                        } else if (rank == 2) {
                            if (info.getDimensions()[1] > 0) {
                                type = double[].class;
                                data = new double[(int) info.getDimensions()[1]];
                                Arrays.fill((double[]) data, Double.NaN);
                            }
                        } else if (rank == 3) {
                            if ((info.getDimensions()[0] > 0) && (info.getDimensions()[1] > 0)) {
                                type = double[][].class;
                                data = new double[(int) info.getDimensions()[0]][(int) info.getDimensions()[1]];
                                for (int i = 0; i < ((double[][]) data).length; i++) {
                                    Arrays.fill(((double[][]) data)[i], Double.NaN);
                                }
                            }
                        }
                    }
                    break;
                default:
                    return;
            }
            if (type == null) {
                return;
            }
        }
        
        if (type.isArray() && (data!=null)){
            Class cls = Arr.getComponentType(data);
            if ((cls!=null) && (Number.class.isAssignableFrom(cls))){
                data = Convert.toPrimitiveArray(data, (cls == BigInteger.class) ? long.class: Convert.getPrimitiveClass(cls));
            }
        }        
        
        if (type == Double.class) {
            writer.float64().writeArrayBlockWithOffset(path, new double[]{(Double) data}, 1, index);
        } else if (type == Float.class) {
            writer.float32().writeArrayBlockWithOffset(path, new float[]{(Float) data}, 1, index);
        } else if (type == BigInteger.class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int64().writeArrayBlockWithOffset(path, new long[]{((BigInteger) data).longValue()}, 1, index);
            } else {
                writer.uint64().writeArrayBlockWithOffset(path, new long[]{((BigInteger) data).longValue()}, 1, index);
            }
        } else if (type == Long.class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int64().writeArrayBlockWithOffset(path, new long[]{(Long) data}, 1, index);
            } else {
                writer.uint64().writeArrayBlockWithOffset(path, new long[]{(Long) data}, 1, index);
            }
        } else if (type == Integer.class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int32().writeArrayBlockWithOffset(path, new int[]{(Integer) data}, 1, index);
            } else {
                writer.uint32().writeArrayBlockWithOffset(path, new int[]{(Integer) data}, 1, index);
            }
        } else if (type == Short.class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int16().writeArrayBlockWithOffset(path, new short[]{(Short) data}, 1, index);
            } else {
                writer.uint16().writeArrayBlockWithOffset(path, new short[]{(Short) data}, 1, index);
            }
        } else if (type == Byte.class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int8().writeArrayBlockWithOffset(path, new byte[]{(Byte) data}, 1, index);
            } else {
                writer.uint8().writeArrayBlockWithOffset(path, new byte[]{(Byte) data}, 1, index);
            }
        } else if (Number.class.isAssignableFrom(type)) {
            writer.float64().writeArrayBlockWithOffset(path, new double[]{((Number) data).doubleValue()}, 1, index);
        } else if (type == Boolean.class) {
            HDF5DataSetInformation info = writer.object().getDataSetInformation(path);
            if (info.getTypeInformation().getDataClass() == HDF5DataClass.FLOAT){
                data = Convert.toDouble(data);
                writer.float64().writeArrayBlockWithOffset(path, new double[]{(Double) data}, 1, index);
            } else {
                BitSet bs = new BitSet(1);
                bs.set(0, (Boolean) data);
                writer.bool().writeBitFieldArrayBlockWithOffset(path, new BitSet[]{bs}, 1, index);
            }
        } else if (type == String.class) {
            writer.string().writeArrayBlock(path, new String[]{(String) data}, index);
        } else if (type == double[].class) {
            writer.float64().writeMatrixBlockWithOffset(path, new double[][]{(double[]) data}, index, 0);
        } else if (type == float[].class) {
            writer.float32().writeMatrixBlockWithOffset(path, new float[][]{(float[]) data}, index, 0);
        } else if ((type == long[].class)||(type==BigInteger[].class)) {            
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int64().writeMatrixBlockWithOffset(path, new long[][]{(long[]) data}, index, 0);
            } else {
                writer.uint64().writeMatrixBlockWithOffset(path, new long[][]{(long[]) data}, index, 0);
            }
        } else if (type == int[].class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int32().writeMatrixBlockWithOffset(path, new int[][]{(int[]) data}, index, 0);
            } else {
                writer.uint32().writeMatrixBlockWithOffset(path, new int[][]{(int[]) data}, index, 0);
            }
        } else if (type == short[].class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int16().writeMatrixBlockWithOffset(path, new short[][]{(short[]) data}, index, 0);
            } else {
                writer.uint16().writeMatrixBlockWithOffset(path, new short[][]{(short[]) data}, index, 0);
            }
        } else if (type == byte[].class) {
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int8().writeMatrixBlockWithOffset(path, new byte[][]{(byte[]) data}, index, 0);
            } else {
                writer.uint8().writeMatrixBlockWithOffset(path, new byte[][]{(byte[]) data}, index, 0);
            }
        } else if (type == boolean[].class) {
            HDF5DataSetInformation info = writer.object().getDataSetInformation(path);
            if (info.getTypeInformation().getDataClass() == HDF5DataClass.FLOAT){
                data = Convert.toDouble(data);
                writer.float64().writeMatrixBlockWithOffset(path, new double[][]{(double[]) data}, index, 0);
            } else {
                BitSet bs = new BitSet(Array.getLength(data));
                for (int i = 0; i < Array.getLength(data); i++) {
                    bs.set(i, ((boolean[]) data)[i]);
                }
                writer.bool().writeBitFieldArrayBlockWithOffset(path, new BitSet[]{bs}, index, 0);                
            }            
        } else if (type == double[][].class) {
            MDDoubleArray array = new MDDoubleArray((double[])Convert.flatten(data), getMatrixShape(data));
            writer.float64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
        } else if (type == float[][].class) {
            MDFloatArray array = new MDFloatArray((float[])Convert.flatten(data), getMatrixShape(data));
            writer.float32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));            
            
        } else if ((type == long[][].class) || (type == BigInteger[][].class)) {        
            MDLongArray array = new MDLongArray((long[])Convert.flatten(data), getMatrixShape(data));
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            } else {
                writer.uint64().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            }
        } else if (type == int[][].class) {
            MDIntArray array = new MDIntArray((int[])Convert.flatten(data), getMatrixShape(data));
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            } else {
                writer.uint32().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            }
        } else if (type == short[][].class) {
            MDShortArray array = new MDShortArray((short[])Convert.flatten(data), getMatrixShape(data));
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int16().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            } else {
                writer.uint16().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            }

        } else if (type == byte[][].class) {
            MDByteArray array = new MDByteArray((byte[])Convert.flatten(data), getMatrixShape(data));
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int8().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            } else {
                writer.uint8().writeMDArrayBlockWithOffset(path, array, getMatrixOffset(index));  
            }
        } else if (type == Object[].class) {
            HDF5CompoundType<Object[]> compoundType = null;
            synchronized (compoundTypes) {
                compoundType = compoundTypes.get(path);
                if (compoundType == null) {
                    throw new IllegalStateException("No compound type defined for path: " + path);
                }
            }
            writer.compound().writeArrayBlockWithOffset(path, compoundType, new Object[][]{(Object[]) data}, index);
        } else {
            throw new UnsupportedOperationException("Not supported type = " + type);
        }
    }   
            
    long[] getMatrixOffset(int index){
        switch (getDepthDimension()){
            case 1: return new long[]{0, index, 0};
            case 2: return new long[]{0, 0, index};
            default: return new long[]{index, 0, 0};
        }
    }
    
    int[] getMatrixShape(Object data){       
        int[] dims = (data == null) ? new int[]{-1, -1} : Arr.getShape(data);        
        switch (getDepthDimension()){
            case 1: return new int[]{dims[0], 1, dims[1]};
            case 2: return new int[]{dims[0], dims[1], 1};
            default: return new int[]{1, dims[0], dims[1]};
        }
    }
    
    Object getMatrixArray(MDAbstractArray array){      
        int[] dimensions = array.dimensions();
        switch (getDepthDimension()){
            case 1: return Convert.reshape(array.getAsFlatArray(), dimensions[0], dimensions[2]);
            case 2: return Convert.reshape(array.getAsFlatArray(), dimensions[0], dimensions[1]);
            default: return Convert.reshape(array.getAsFlatArray(), dimensions[1], dimensions[2]);
        }
    }
        
    int[] get3dMatrixDims(Object array){
        int[] dims = Arr.getShape(array);  
        switch (getDepthDimension()){
            case 1: return new int[]{dims[1], dims[0], dims[2]};
            case 2: return  new int[]{dims[1], dims[2], dims[0]};
            default: return new int[]{dims[0], dims[1], dims[2]};
        }        
    }    
    
    
    @Override
    public void setItem(String path, Object data, Class type,long[] index,  int[] shape) throws IOException{                
        assertOpenOutput();
        if (type == double[].class) {
            MDDoubleArray array = new MDDoubleArray((double[])data, shape);
            writer.float64().writeMDArrayBlockWithOffset(path, array, index);  
        } else if (type == float[].class) {
            MDFloatArray array = new MDFloatArray((float[])data, shape);
            writer.float32().writeMDArrayBlockWithOffset(path, array, index);            
            
        } else if (type == long[].class) {
            MDLongArray array = new MDLongArray((long[])data, shape);
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int64().writeMDArrayBlockWithOffset(path, array, index);  
            } else {
                writer.uint64().writeMDArrayBlockWithOffset(path, array, index);  
            }
        } else if (type == int[].class) {
            MDIntArray array = new MDIntArray((int[])data, shape);
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int32().writeMDArrayBlockWithOffset(path, array, index);  
            } else {
                writer.uint32().writeMDArrayBlockWithOffset(path, array, index);  
            }
        } else if (type == short[].class) {
            MDShortArray array = new MDShortArray((short[])data, shape);
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int16().writeMDArrayBlockWithOffset(path, array, index);  
            } else {
                writer.uint16().writeMDArrayBlockWithOffset(path, array, index);  
            }

        } else if (type == byte[].class) {
            MDByteArray array = new MDByteArray((byte[])data, shape);
            if (writer.object().getDataSetInformation(path).isSigned()) {
                writer.int8().writeMDArrayBlockWithOffset(path, array, index);  
            } else {
                writer.uint8().writeMDArrayBlockWithOffset(path, array, index);  
            }
        } else {
            throw new UnsupportedOperationException("Not supported type = " + type);
        }        
    }    

    @Override
    public void setAttribute(String path, String name, Object value, Class type, boolean unsigned) {         
        assertOpenOutput();
        if (type.isArray()) {
            Class componentType = type.getComponentType();
            if (componentType == double.class) {
                writer.float64().setArrayAttr(path, name, (double[]) value);
            } else if (componentType == float.class) {
                writer.float32().setArrayAttr(path, name, (float[]) value);
            } else if (componentType == long.class) {
                if (unsigned) {
                    writer.uint64().setArrayAttr(path, name, (long[]) value);
                } else {
                    writer.int64().setArrayAttr(path, name, (long[]) value);
                }
            } else if (componentType == int.class) {
                if (unsigned) {
                    writer.uint32().setArrayAttr(path, name, (int[]) value);
                } else {
                    writer.int32().setArrayAttr(path, name, (int[]) value);
                }
            } else if (componentType == short.class) {
                if (unsigned) {
                    writer.uint16().setArrayAttr(path, name, (short[]) value);
                } else {
                    writer.int16().setArrayAttr(path, name, (short[]) value);
                }
            } else if (componentType == byte.class) {
                if (unsigned) {
                    writer.int8().setArrayAttr(path, name, (byte[]) value);
                } else {
                    writer.uint8().setArrayAttr(path, name, (byte[]) value);
                }
            } else if (componentType == String.class) {
                //TODO: JHDF5 is throwing an exception if array length is 0. Empty array should be supported.
                if (((String[]) value).length > 0) {
                    writer.string().setArrayAttr(path, name, (String[]) value);
                }
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        } else {
            if (type == Double.class) {
                writer.float64().setAttr(path, name, (double) value);
            } else if (type == Float.class) {
                writer.float32().setAttr(path, name, (float) value);
            } else if (type == Long.class) {
                if (unsigned) {
                    writer.uint64().setAttr(path, name, (long) value);
                } else {
                    writer.int64().setAttr(path, name, (long) value);
                }
            } else if (type == BigInteger.class) {
                if (unsigned) {
                    writer.uint64().setAttr(path, name, ((BigInteger) value).longValue());
                } else {
                    writer.int64().setAttr(path, name, ((BigInteger) value).longValue());
                }
            } else if (type == Integer.class) {
                if (unsigned) {
                    writer.uint32().setAttr(path, name, (int) value);
                } else {
                    writer.int32().setAttr(path, name, (int) value);
                }
            } else if (type == Short.class) {
                if (unsigned) {
                    writer.uint16().setAttr(path, name, (short) value);
                } else {
                    writer.int16().setAttr(path, name, (short) value);
                }
            } else if (type == Byte.class) {
                if (unsigned) {
                    writer.int8().setAttr(path, name, (byte) value);
                } else {
                    writer.uint8().setAttr(path, name, (byte) value);
                }
            } else if (type == Boolean.class) {
                writer.bool().setAttr(path, name, (boolean) value);
            } else if (type == String.class) {
                writer.string().setAttr(path, name, (String) value);
            } else {
                throw new UnsupportedOperationException("Not supported type = " + type);
            }
        }
    }
}
