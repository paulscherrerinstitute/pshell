package ch.psi.pshell.core;

import ch.psi.pshell.device.Register.RegisterArray;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.epics.CAS;
import ch.psi.utils.Str;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ChannelAccessServer extends RegisterBase implements RegisterArray {
    final String name;
    final String channel;
    final boolean asString;
    final boolean background;
    final int maxSize ;
    
    String val;
    boolean debug;
    CAS cas;    

    public  ChannelAccessServer(String name, String channel, boolean background, int maxSize){
        this.name=name;
        this.channel=channel;
        this.asString=false;
        this.background=background;
        this.maxSize=maxSize;        
        
        val = "INIT";
        debug=false;
        cas = null;
    }
    
    public  ChannelAccessServer(String name, String channel, boolean background){
        this.name=name;
        this.channel=channel;
        this.asString=true;
        this.background=background;
        this.maxSize=1;        
        
        val = "INIT";
        debug=false;
        cas = null;        
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        try {
            if (asString){
                cas = new CAS(channel, this, "string");
            } else{ 
                cas = new CAS(channel, this, "byte");
            }                              
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        val = "READY";    
    }    
    
    @Override
    public int  getSize(){
        return maxSize;
    }
    
    @Override
    protected void doWrite(Object value) throws IOException, InterruptedException {
        val = "RUNNING";
        String cmd;
        try{
            if (asString){
                cmd = Str.toString(Array.get(value, 0));                        
            } else {
                cmd = new String((byte[]) value, "UTF-8");
            }
            if (debug){   
                getLogger().info("WRITE: " + cmd);
            }
                      
             BiFunction evalCallback = new BiFunction() {
                @Override
                public Object apply(Object ret, Object ex) {
                        if (ex != null){
                            val = "ERR:" + ((Throwable)ex).getMessage();  
                        } else {
                            val = "RET:" + ret;  
                        }
                    if (debug){
                        getLogger().info(val);
                    }
                    return ret;
                }
             };
            if (background){
                Context.getInstance().evalLineBackgroundAsync(cmd).handle(evalCallback);
            } else {
                Context.getInstance().evalLineAsync(cmd).handle(evalCallback);
            }
        } catch (Exception ex){
            val = "EXC: " + ex.getMessage();
            //val = self.val[0:self.max_size]
            if (debug){
                getLogger().info(val);
            }
        }
    }

    @Override
    protected Object doRead() throws IOException, InterruptedException {
        if (debug){
            getLogger().info("READ: " + val);
        }
        if (asString){
            return val;
        } else {
            return val.getBytes("UTF-8");
        }
    }
    
    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean value) {
        debug = value;
    }    
    
    @Override
    protected void doClose() throws IOException {
        if (cas!=null){
            try {
                cas.close();
            } catch (Exception ex) {
                Logger.getLogger(ChannelAccessServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            cas = null;
        }
        super.doClose();                
    }    
    
}
