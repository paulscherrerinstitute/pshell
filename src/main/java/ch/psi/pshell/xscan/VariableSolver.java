package ch.psi.pshell.xscan;

import ch.psi.utils.Convert;
import java.util.List;

/**
 *
 */
public interface VariableSolver {

    default double getDouble(String var){
        return getDouble(var, 0.0);
    }
    
    default double getDouble(String var, double defaultValue){
        if (var!=null){
            try{
                return Double.parseDouble(var);
            } catch (Exception ex){
            }            
            
            try{
                return ((Number) ProcessorXScan.getInterpreterVariable(var)).doubleValue();
            } catch (Exception ex){            
            }    
        }
        return defaultValue;
    }
    
    default int getInt(String var){
        return getInt(var, 0);
    }
    
    default int getInt(String var, int defaultValue){
        try{
            return Integer.parseInt(var);
        } catch (Exception ex){
        }          
        if (var!=null){
            try{
                return ((Number) ProcessorXScan.getInterpreterVariable(var)).intValue();
            } catch (Exception ex){            
            }   
        }
        return defaultValue;
    }    
    
    default boolean getBoolean(String var){
        return getBoolean(var, false);
    }
    
    default boolean getBoolean(String var, boolean defaultValue){
        if (var!=null){
            try{
                return Boolean.parseBoolean(var);
            } catch (Exception ex){
            }               
            try{
                return ((Boolean) ProcessorXScan.getInterpreterVariable(var));
            } catch (Exception ex){               
            }    
        }
        return defaultValue;
    }        
    
    default String getString(String var){
        return getString(var, var);
    }
    
    default String getString(String var, String defaultValue){
        if (var!=null){
            try{
                String ret = (String)ProcessorXScan.getInterpreterVariable(var);
                if (ret!=null){
                    return ret;
                }
            } catch (Exception ex){            
            }            
        }        
        return defaultValue;
    }
    
    
    default String[] getStringArray(String var, String[] defaultValue){
        if (var!=null){
            try{
                Object obj = ProcessorXScan.getInterpreterVariable(var);
                if (obj.getClass().isArray() || (obj instanceof List)){
                    return Convert.toStringArray(obj);
                } else if (obj instanceof String){
                    return ((String)obj).trim().split(" ");
                }
            } catch (Exception ex){            
            }            
        }        
        return defaultValue;
    }    

}
