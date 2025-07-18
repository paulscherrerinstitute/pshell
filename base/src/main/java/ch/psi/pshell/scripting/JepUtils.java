package ch.psi.pshell.scripting;

import ch.psi.pshell.utils.Convert;
import jep.NDArray;

/**
 *
 */
public class JepUtils {
    public static Object toJavaArray(NDArray array){
        int[] dims =  array.getDimensions();
        Object ret = array.getData();
        if (dims.length>1){
            ret=Convert.reshape(ret, dims);                
        }                            
        return ret;
    }    
    public static String getPythonHome() {
         String ret = System.getenv("PYTHONHOME");
         if ((ret==null) || ret.isBlank()){
             return null;
         }
         return ret.trim();
    }            
    
}
