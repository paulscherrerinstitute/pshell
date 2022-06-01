package ch.psi.pshell.xscan;

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
                return ((Boolean) ProcessorXScan.getInterpreterVariable(var));
            } catch (Exception ex){               
            }    
        }
        return defaultValue;
    }        
    
    default String getString(String var){
        return getString(var, "");
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

}
