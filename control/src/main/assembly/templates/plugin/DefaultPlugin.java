import ch.psi.pshell.framework.Plugin;
import ch.psi.pshell.utils.State;

/**
 *
 */
public class DefaultPlugin implements Plugin {
   
    //Overridable callbacks
    @Override
    public void onStart() {        
    }

    @Override
    public void onStop() {        
    }

    @Override
    public void onInitialize(int runCount) {

    }

    @Override
    public void onStateChange(State state, State former) {

    }

    @Override
    public void onExecutedFile(String fileName, Object result) {
    }    
    
}
