package ch.psi.pshell.scripting;

/**
 * The script file extensions
 */
public enum ScriptType {

    py,
    groovy,
    js;

    public String getLineCommentMarker() {
        switch (this) {
            case py:
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
}
