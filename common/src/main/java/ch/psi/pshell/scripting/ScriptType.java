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
        return switch (this) {
            case py, cpy -> "#";
            default -> "//";
        };
    }

    public static ScriptType getDefault() {
        try {
            return ScriptType.valueOf(System.getProperty("build.type"));
        } catch (Exception ex) {
        }
        return ScriptType.py;
    }

    public String getExtension(){
        return (this==cpy) ? "py" : toString();
    }
    
    public String getDefaultStartupFile(){
        String prefix = (this==cpy) ? "startup_c" : "startup";
        return prefix + "." + getExtension();
    }
    
    public boolean isPython(){
        return (this==py) || (this==cpy);
    }            
            
}
