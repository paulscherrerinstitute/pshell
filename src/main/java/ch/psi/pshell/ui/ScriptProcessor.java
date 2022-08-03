
package ch.psi.pshell.ui;

import java.util.Map;

/**
 *
 */
public abstract class ScriptProcessor extends PanelProcessor {
    
    public abstract String getScript();
    
    public abstract Map<String, Object> getArgs();
    
    @Override
    public boolean canSave() {
        return false;
    }  
    
    @Override
    public void execute() throws Exception{
        //Default implementation uses getScript and getArgs
        if (getScript()==null){
            throw new Exception ("Execution procedure not implemented");
        }
        Map<String, Object> args = getArgs();
        runAsync(getScript(), args);
    } 
}
