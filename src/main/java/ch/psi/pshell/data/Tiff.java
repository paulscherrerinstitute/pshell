package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.Map;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.io.FileSaver;
import ij.io.Opener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class Tiff {

    public static void save(Object array, String filename) throws IOException {
        save(array, filename, false);
    }

    public static void save(Object array, String filename, boolean parallel) throws IOException {
        save(array, filename, parallel, null);
    }

    public static void save(Object array, String filename, Map<String, String> metadata) throws IOException {
        save(array, filename, false, metadata);
    }

    public static void save(Object array, String filename, boolean parallel, Map<String, String> metadata) throws IOException {
        if (Arr.getRank(array) != 2) {
            throw new IOException("Invalid array rank: " + Arr.getRank(array));
        }
        array = Convert.toPrimitiveArray(array);
        int[] shape = Arr.getShape(array);
        int height = shape[0];
        int width = shape[1];
        array = Convert.flatten(array);
        save(array, width, height, filename, parallel, metadata);
    }

    public static void save(Object array, int width, int height, String filename, boolean parallel, Map<String, String> metadata) throws IOException {
        if (Arr.getRank(array) != 1) {
            throw new IOException("Invalid array rank: " + Arr.getRank(array));
        }

        ImageProcessor proc;
        Class type = Arr.getComponentType(array);

        if (type == byte.class) {
            proc = new ByteProcessor(width, height, (byte[]) array);
        } else if (type == short.class) {
            proc = new ShortProcessor(width, height, (short[]) array, null);
        } else if (type == float.class) {
            proc = new FloatProcessor(width, height, (float[]) array);
        } else if (type == int.class) {
            proc = new FloatProcessor(width, height, (int[]) array);
        } else if (type == double.class) {
            proc = new FloatProcessor(width, height, (double[]) array);
        } else {
            throw new IOException("Invalid array type: " + type);
        }

        ImagePlus ip = new ImagePlus("img", proc);

        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileSaver fs = new FileSaver(ip);
        if (metadata==null) {
            metadata = new HashMap<>();
        }
        metadata.put("type", type.toString());
        String info = "";
        for (Object key : metadata.keySet()) {
            if (info.length() > 0) {
                info = info + "\n";
            }
            info = info + key + ": " + Str.toString(metadata.get(key),100);
        }
        ip.setProperty("Info", info);

        if (parallel) {
            Callable callable = new Callable() {
                @Override
                public Object call() throws Exception {
                    return fs.saveAsTiff(filename);
                }
            };
            Threading.fork(new Callable[]{callable});
        } else {
            fs.saveAsTiff(filename);
        }
    }
    
    
    public static Object load(String filename) throws IOException{
        File file = new File(filename);
        if (!file.isFile()){
            throw new IOException("Invalid file name: " + filename);
        }
        
        Opener opener = new Opener();
        ImagePlus ip = opener.openTiff(filename, 1);      
        ImageProcessor proc = ip.getProcessor();
        Object ret = null;
        Map<String, String> metadata = new HashMap<>();
        String info = (String) ip.getProperty("Info");
        if ((info!=null) && !info.isBlank()){            
            for (String row : info.split("\n")){
                int index = row.indexOf(": ");
                String key = row.substring(0, index);
                String value = row.substring(index+2);
                metadata.put(key, value);
            }
            
        }
        String type = metadata.getOrDefault("type", "float");
        if (type.equals("float") || type.equals("double") || type.equals("int")){
            ret =  proc.getFloatArray();
            if (type.equals("double")){
                ret=Convert.toPrimitiveArray(ret, double.class);
            } else if (type.equals("int")){ //int array does not work
                ret=Convert.toPrimitiveArray(ret, int.class);
            }
        } else{
            ret =  proc.getIntArray();
            if (type.equals("byte")){
                ret=Convert.toPrimitiveArray(ret, byte.class);
            } else if (type.equals("short")){
                ret=Convert.toPrimitiveArray(ret, short.class);
            } 
        }
        
        return Convert.transpose(ret); //Can avoid this?
    }
    
    public static void main(String[] args) throws Exception {        
        String filename = "/Users/gobbo_a/tst.tiff";
        
        Object[] data2=null;
        Object[][] data  = {
            new int[][]{
                    new int[] {1,2,3,4},
                    new int[] {5,6,7,8},
                    new int[] {9,10,11,12},
            }     ,              
            new float[][]{
                    new float[] {1,2,3,4},
                    new float[] {5,6,7,8},
                    new float[] {9,10,11,12},
            }   ,     
            new double[][]{
                    new double[] {1,2,3,4},
                    new double[] {5,6,7,8},
                    new double[] {9,10,11,12},
            }     ,               
            new byte[][]{
                    new byte[] {1,2,3,4},
                    new byte[] {5,6,7,8},
                    new byte[] {9,10,11,12},
            }     ,   
            new short[][]{
                    new short[] {1,2,3,4},
                    new short[] {5,6,7,8},
                    new short[] {9,10,11,12},
            }        
        };

        for (Object[] data1 : data){
            Tiff.save(data1, filename);        
            data2=  (Object[]) Tiff.load(filename);                
            System.out.println(data2);
            System.out.println(Arrays.deepEquals(data1,data2));
        }
               
    }
        
}
