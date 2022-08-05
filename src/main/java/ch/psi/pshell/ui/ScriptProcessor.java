
package ch.psi.pshell.ui;

import ch.psi.pshell.core.Context;
import ch.psi.utils.IO;
import java.io.File;
import java.util.Map;

/**
 *
 */
public abstract class ScriptProcessor extends PanelProcessor {
    
    public abstract String getScript();
    
    public abstract Map<String, Object> getArgs();
    
    private volatile boolean running;
    
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
        running = true;
        runAsync(getScript(), args).handle((ok, ex) -> {
            running = false;
            return ok;
        });
    } 
    
    public boolean isRunning(){
        return running;
    }
    
    @Override
    public void queue() throws Exception{
        getQueueProcessor().addNewFile(getScript(), getArgs());
    }
    
    public void runNext() throws Exception{
        String filename = getScript();
        if (filename==null){
            throw new Exception("File not saved");
        }           
        
        File file = new File(filename);
        if (IO.isSubPath(filename, Context.getInstance().getSetup().getScriptPath())) {
            //If in script folder then use only relative
            file = new File(IO.getRelativePath(filename, Context.getInstance().getSetup().getScriptPath()));
        }                     
        
        if (Context.getInstance().getState().isProcessing()) {
            App.getInstance().evalFileNext(file, getArgs());
        } else {
            App.getInstance().evalFile(file, getArgs());
        }
    }       

    
}
