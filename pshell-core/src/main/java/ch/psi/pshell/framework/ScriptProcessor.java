
package ch.psi.pshell.framework;

import ch.psi.pshell.utils.IO;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;

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
    
    protected boolean isScriptExceptionShown(){
        return (getView()==null) ? true : getView().getScriptPopupMode() != MainFrame.ScriptPopupMode.None;
    }
    
    @Override
    public void execute() throws Exception{
        //Default implementation uses getScript and getArgs
        if (getScript()==null){
            throw new Exception ("Execution procedure not implemented");
        }
        Map<String, Object> args = getArgs();
        
        
        boolean showException = isScriptExceptionShown();
            
        running = true;        
        try{
            onStartingExecution(args);
        } catch (Exception e){ 
            getLogger().log(Level.WARNING, null, e);
        }         
        
        runAsync(getScript(), args).handle((ret, ex) -> {
            if (ex != null) {               
                if (showException) {
                    if (!Context.isAborted()) {     
                        showException((Exception)ex);
                    }  
                }
            }
            try{
                onFinishedExecution(args, ret, ex);
            } catch (Exception e){ 
                getLogger().log(Level.WARNING, null, e);
            } finally{
                running = false;
            }
            return ret;
        });
        
    } 
    
    protected void onStartingExecution(Map<String, Object> args) throws Exception{        
    }
    
    protected void onFinishedExecution(Map<String, Object> args, Object ret, Throwable t) throws Exception{   
    }
    
    
    public boolean isRunning(){
        return running;
    }
    
    @Override
    public boolean canPause() {
        return true;
    }  
    
    public void pause() {
        try{
            Context.getSequencer().pause();
        } catch (Exception e){ 
            getLogger().log(Level.WARNING, null, e);
        }   
    }
    
    public void resume() {
        try{
            Context.getSequencer().resume();
        } catch (Exception e){ 
            getLogger().log(Level.WARNING, null, e);
        }            
    }    
    
    

    @Override
    public void queue() throws Exception{
        //By default queue script and arguments insted of the filename
        queue(false);
    }
    
    public void queue(boolean filename) throws Exception{
        if (filename){
            super.queue();
        } else {
            getQueueProcessor().addNewFile(getScript(), getArgs());
        }        
    }
    
    public void runNext() throws Exception{
        String filename = getScript();
        if (filename==null){
            throw new Exception("File not saved");
        }           
        
        File file = new File(filename);
        if (IO.isSubPath(filename, Setup.getScriptsPath())) {
            //If in script folder then use only relative
            file = new File(IO.getRelativePath(filename, Setup.getScriptsPath()));
        }                     
        
        if (Context.getState().isProcessing()) {
            Context.getApp().evalFileNext(file, getArgs());
        } else {
            Context.getApp().evalFile(file, getArgs());
        }
    }       

}
