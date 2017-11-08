package ch.psi.pshell.device;

import java.io.IOException;

/**
 *
 */
public abstract class SettlingCondition {
    int latency;
    Register register;
    
    protected void setRegister(Register register){
        this.register = register;
    }
    
    public Register getRegister(){
        return register;
    }

    public void setLatency(int latency){
        this.latency = latency;
    }
    
    public int getLatency(){
        return latency;
    }
    
    public Object getValue(){
        if (register!=null) {
            return register.take();
        }
        return null;
    }
        
    public void waitSettled() throws IOException, InterruptedException{
        if (latency>0){
            Thread.sleep(latency);
        }
        doWait();
    }
    
    abstract protected void doWait() throws IOException, InterruptedException;

}
