package ch.psi.pshell.app;
    
import ch.psi.pshell.utils.IO;
import ch.psi.pshell.utils.Str;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface Option {    
    default public String toArgument(){
        return toString().toLowerCase();
    }
    
    default public String toProperty(){
        return "ch.psi.pshell."+toString().replace("_", ".").toLowerCase();
    }
    
    default public String toEnvVar(){
        return "PSHELL_"+toString().toUpperCase();
    }
    
    default public void setProperty(String value){
        System.setProperty(toProperty(), value);          
    }
    
    static Map<Option, String> forcedOptions = new HashMap<>();
        
    
    default public String getString(String defaultValue){    
        String propertyValue = null;
        String argument = toArgument();
        String property = toProperty();        
        String envVar = toEnvVar();

        if (forcedOptions.containsKey(this)){
            propertyValue = forcedOptions.get(this);
        }
        if (propertyValue==null || propertyValue.isBlank()){
            if (argument!=null){
                if (App.isArgumentDefined(argument)) {
                    return App.getArgumentValue(argument);
                }
            }
            if (envVar!=null){
                String val = System.getenv(envVar);
                if ((val!=null) && (!val.isBlank())){
                    return val.trim();
                }
            }
            if (property!=null){
                propertyValue = System.getProperty(property);
            }
        }
        if ((propertyValue != null) && (!propertyValue.isBlank())) {
            return propertyValue.trim();
        }
        return defaultValue;
    }
    
    default public Boolean getBool(Boolean defaultValue){       
        String propertyValue = null;
        String argument = toArgument();
        String property = toProperty();        
        String envVar = toEnvVar();
        
        if (forcedOptions.containsKey(this)){
            propertyValue = forcedOptions.get(this);
        }
        if (propertyValue==null || propertyValue.isBlank()){
            if (argument!=null){
                if (App.hasArgument(argument)) {
                    return App.getBoolArgumentValue(argument);
                }
               if (App.hasAditionalArgument(argument)){
                   return true;
               }
            }
            if (envVar!=null){
                String val = System.getenv(envVar);
                if ((val!=null) && (!val.isBlank())){
                    val = val.trim();
                    return !(val.equalsIgnoreCase("false")) && !(val.equalsIgnoreCase("0"));
                }
            }
            
            if (property!=null){
                propertyValue = System.getProperty(property);
            }
        }
        if ((propertyValue != null) && (!propertyValue.isBlank())) {
            propertyValue = propertyValue.trim().toLowerCase();
            return !(propertyValue.equalsIgnoreCase("false")) && !(propertyValue.equalsIgnoreCase("0"));
        }        
        return defaultValue;
    }
    
    default public Integer getInt(Integer defaultValue){            
        try{
            String str = getString(null);
            return Integer.valueOf(str);
        } catch (Exception ex){            
            return defaultValue;
        }        
    }    
    
    default public List<String> getStringList(){             
        var ret = new ArrayList<String>();
        String argument = toArgument();
        String property = toProperty();        
        String envVar = toEnvVar();        
        
        if (argument!=null){
            ret.addAll(App.getArgumentValues(argument));
        }
        String separator = ",";
        if (envVar!=null){            
            String val = System.getenv(envVar);
            if ((val!=null) && (!val.isBlank())){
                for (String str : Str.splitRemoveQuotes(val.trim(),separator)){
                    if (str.isBlank()){
                        ret.add(str.trim());
                    }
                }
            }
        }
        if (property!=null){
            String val = System.getProperty(property);
            if ((val!=null) && (!val.isBlank())){
                for (String str : Str.splitRemoveQuotes(val.trim(),separator)){
                    if (str.isBlank()){
                        ret.add(str.trim());
                    }
                }
            }
        }
        return ret;
    }
    
    default public File getPath() {
        String option = toString();
        if (option != null) {
            option =Setup.expandPath(option);
            File f = new File(option);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }
    
        
    default public String getPropertyFile(String defaultValue){    
        String ret =getString(defaultValue) ;
        if (IO.getExtension(ret).isEmpty()) {
            ret += ".properties";
        }      
        return ret;
    }
    
    
    default public boolean defined(){    
        return getString(null) != null;
    }
    
    default public boolean hasValue(){    
        return !getString("").isBlank();
    }
    
    default public void set() {
        force("true");
    }
    
    default public void reset() {
        force("false");
    }

    default public void force(String value){ 
        forcedOptions.put(this, value);
    }
    
    default public void add(String shortName, String desc){
        add(shortName, desc, "");
    }
    
    default public void add(String shortName, String desc, String argName){
        App.addOption(shortName, toArgument(),  desc, argName);
    }
    
}
