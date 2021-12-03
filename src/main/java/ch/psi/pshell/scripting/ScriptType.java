package ch.psi.pshell.scripting;

/**
 * The script file extensions
 */
public enum ScriptType {

    py,
    groovy,
    js,
    cpy;

    public String getLineCommentMarker() {
        switch (this) {
            case py:
            case cpy:
                return "#";
            default:
                return "//";
        }
    }

    public static ScriptType getDefault() {
        try {
            return ScriptType.valueOf(System.getProperty("pshell.build.type"));
        } catch (Exception ex) {
        }
        return ScriptType.py;
    }

    public String getExtension(){
        return (this==cpy) ? "py" : toString();
    }
    
    public String getDefaultStartupFile(){
        String prefix = (this==cpy) ? "startup_jep" : "startup";
        return prefix + "." + getExtension();
    }
    
    public boolean isPython(){
        return (this==py) || (this==cpy);
    }    
    
    public String getSignatureLimitToken(){
        switch (this) {
            case cpy:
                return " ->";
            default:
                return " ";
        }        
    }
    
            
}
