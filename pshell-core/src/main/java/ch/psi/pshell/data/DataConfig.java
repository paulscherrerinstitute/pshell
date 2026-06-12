
package ch.psi.pshell.data;

import ch.psi.pshell.utils.Str;


public record DataConfig(
    String format,
    String layout, 
    Boolean truncate,
    Integer depthDimension,
    Boolean textEmbededAttrs,
    String textItemSeparator,
    String textArraySeparator,
    String textLineSeparator,
    Boolean textFinalSeparator
    ) {
    
    boolean isNull(String str){
        return (str==null) ||  str.trim().equals(Str.toString(null));
    }

    boolean isEmpty(String str){
        return isNull(str) || str.isBlank();
    }

    public String format(){
        return isEmpty(format) ? null : format;
    }
    
    public String layout(){
        return isEmpty(layout) ? null : layout;
    }
    
    public Integer depthDimension(){
        if (depthDimension==null){
           return null; 
        } 
        if ((depthDimension<0) || (depthDimension>2)){
            return 0;
        }
        return depthDimension;
    }
    
    public String textItemSeparator(){
        return isNull(textItemSeparator) ? null : textItemSeparator;
    }

    public String textArraySeparator(){
        return isNull(textArraySeparator) ? null : textArraySeparator;
    }

    public String dataTextLineSeparator(){
        return isNull(textLineSeparator) ? null : textLineSeparator;
    }        
            
    public DataConfig() {       
        this(null, null, null, null, null, null, null, null, null);
    }        
    
    public DataConfig(String format, String layout){
        this(format, layout, null, null, null, null, null, null, null);
    }
    
}
