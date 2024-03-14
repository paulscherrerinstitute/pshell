package ch.psi.pshell.data;

import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import ch.psi.utils.Str;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.Map;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.FloatProcessor;
import ij.io.FileSaver;
import ij.io.Opener;
import java.io.File;
import java.lang.reflect.Array;
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
        return load(filename, 0);
    }
    
    public static Object load(String filename, int index) throws IOException{
        File file = new File(filename);
        if (!file.isFile()){
            throw new IOException("Invalid file name: " + filename);
        }
        
        Opener opener = new Opener();
        ImagePlus ip = opener.openTiff(filename, index +1);      
        ImageProcessor proc = ip.getProcessor();
        Object ret = null;
        Map<String, String> metadata = new HashMap<>();
        String info = (String) ip.getProperty("Info");
        if ((info!=null) && !info.isBlank()){            
            for (String row : info.split("\n")){
                int i = row.indexOf(": ");
                String key = row.substring(0, i);
                String value = row.substring(i+2);
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
    
    
    public static void saveStack(Object array, String filename) throws IOException {
        saveStack(array, filename, false);
    }

    public static void saveStack(Object array, String filename, boolean parallel) throws IOException {
        saveStack(array, filename, parallel, null);
    }

    public static void saveStack(Object array, String filename, Map<String, String> metadata) throws IOException {
        saveStack(array, filename, false, metadata);
    }

    public static void saveStack(Object array, String filename, boolean parallel, Map<String, String> metadata) throws IOException {
        if (Arr.getRank(array) != 3) {
            throw new IOException("Invalid array rank: " + Arr.getRank(array));
        }
        array = Convert.toPrimitiveArray(array);
        Class type = Arr.getComponentType(array);
        int[] shape = Arr.getShape(array);        
        int images = shape[0];
        int height = shape[1];
        int width = shape[2];
        Object out = Array.newInstance(type, images, height * width);
        for(int i=0; i<images; i++){
            Object arr = Convert.flatten(Array.get(array,i));
            Array.set(out,i,arr);
        }
        saveStack(out, width, height, filename, parallel, metadata);
    }

    public static void saveStack(Object array, int width, int height, String filename, boolean parallel, Map<String, String> metadata) throws IOException {

        ImageProcessor proc;
        ImageStack stack = new ImageStack(width, height);
        Class type = Arr.getComponentType(array);
        int[] shape = Arr.getShape(array);        
        int images = shape[0];
        int size = shape[1];        
        for (int i=0; i<images; i++){
            Object arr = Array.get(array, i);
            if (type == byte.class) {
                proc = new ByteProcessor(width, height, (byte[]) arr);
            } else if (type == short.class) {
                proc = new ShortProcessor(width, height, (short[]) arr, null);
            } else if (type == float.class) {
                proc = new FloatProcessor(width, height, (float[]) arr);
            } else if (type == int.class) {
                proc = new FloatProcessor(width, height, (int[]) arr);
            } else if (type == double.class) {
                proc = new FloatProcessor(width, height, (double[]) arr);
            } else {
                throw new IOException("Invalid array type: " + type);
            }
            ImagePlus ip = new ImagePlus("img", proc);
            stack.addSlice("Image_"+i, ip.getProcessor());
        }

        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        ImagePlus ip = new ImagePlus("Image Stack", stack);
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
                    return fs.saveAsTiffStack(filename);
                }
            };
            Threading.fork(new Callable[]{callable});
        } else {
            fs.saveAsTiffStack(filename);
        }
    }
}
