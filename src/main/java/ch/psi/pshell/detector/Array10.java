package ch.psi.pshell.detector;

import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterMatrix;
import ch.psi.utils.BufferConverter;
import ch.psi.utils.Convert;
import ch.psi.utils.EncoderJson;
import ch.psi.utils.State;
import ch.psi.utils.Type;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
        
/**
 * Array streaming protocol based on ZMQs
 */
public class Array10 extends DeviceBase implements Readable, Cacheable, Readable.ReadableType{
    final String address;
    final int mode;
    final Array10Array devArray;
    final Array10Matrix devMatrix;
    volatile int message_count;  
    Thread thread;
    
    public Array10(String name, String address, int mode){
        super(name);
        this.address = address;
        this.mode = mode;        
        this.devArray = new Array10Array();
        this.devMatrix = new Array10Matrix();
        addChild(devArray);
        addChild(devMatrix);
        message_count=0;
    }
    
    public class Array10Array extends ReadonlyAsyncRegisterBase implements ReadonlyRegisterArray{        
        Array10Array(){
            super(Array10.this.getName() + "_array");
        }
        
        @Override
        public int getSize() {
            Object cache = take();
            if ((cache == null) || (!cache.getClass().isArray())) {
                return 0;
            }
            return Array.getLength(cache);
        }
        
        void set(Object data){
            onReadout(data);
        }
    }
    
    public class Array10Matrix extends ReadonlyAsyncRegisterBase implements ReadonlyRegisterMatrix{        
        int[] shape;
        int modulo =1;
        Array10Matrix(){
            super(Array10.this.getName()+ "_matrix");
        }
        
        @Override
        public int getWidth() {
            return (shape==null) ? 0 : shape[1];
        }
        
        @Override
        public int getHeight() {
            return (shape==null) ? 0 : shape[0];
        }
        
        public int getModulo(){
            return modulo;
        }

        public void setModulo(int modulo){
            this.modulo = modulo;
        }

        void set(Object data, int[] shape){
            if (message_count % modulo == 0){
                this.shape = shape;
                if ((data!=null)&&(shape!=null)&&(shape.length==2)){
                    onReadout(Convert.reshape(data, shape));
                }
            }
        }
    }    

    @Override
    protected void doInitialize() throws IOException, InterruptedException{
        stop();
        super.doInitialize();
        message_count=0;
        start();                
    }
            

    public void start(){
        if (thread==null){
            getLogger().info("Starting");
            thread = new Thread(() -> {
                receiverTask();
            });
            thread.setName("Array10 receiver: " + getName());
            thread.setDaemon(true);
            setState(State.Busy);
            thread.start();
        }
    }
    
    
    public void stop(){
        if (thread!=null){
            getLogger().info("Stopping");
            try{
                thread.interrupt();
            } finally {
                thread=null;
                setState(State.Ready);
            }
        }
    }

    private void receiverTask(){
        getLogger().info("Enter rx thread");
        Context context = null;
        Socket socket = null;
        try{
            context = ZMQ.context(1);
            socket = context.socket(mode);
            socket.connect(address);
            if (mode == ZMQ.SUB){
                socket.subscribe("");
            }
            getLogger().info("Running " +  mode + " "  + address);
            while ((!Thread.currentThread().isInterrupted()) && (getState().isRunning())) {
                byte[] header_arr = socket.recv(ZMQ.NOBLOCK);
                if (header_arr !=null){                                   
                    try{
                        String json = new String(header_arr, StandardCharsets.UTF_8);
                        Map<String, Object> header = (Map) EncoderJson.decode(json, Map.class);                        
                        int[] shape = (int[]) Convert.toPrimitiveArray(header.getOrDefault("shape", new ArrayList()), int.class);
                        String dtype = (String) header.getOrDefault("type", "int8");
                        byte[] data_arr = socket.recv(); 
                        if (data_arr!=null){
                            Object data=BufferConverter.fromArray(data_arr, Type.fromKey(dtype));
                            Map<String, Object> value = new HashMap<>();
                            value.put("header", header);
                            value.put("data", data);
                            value.put("id", message_count);
                            message_count++;
                            setCache(value);
                            if (devArray!=null){
                               devArray.set(data);
                            }
                            if (devMatrix!=null){
                               devMatrix.set(data, shape);
                            }                        }
                    } catch (Exception ex){
                        getLogger().log(Level.WARNING, null, ex);
                    }
                } else {
                    Thread.sleep(10);
                }
            }
        } catch (Exception ex){
            getLogger().log(Level.WARNING, null, ex);
        } finally {
            if (socket!=null){
               socket.close();
            }  
            if (context!=null){
               context.term();
            }
            getLogger().info("Quit rx thread");     
        }
    }
    
    public Array10Array getDevArray(){
        return devArray;
    }
    
    public Array10Matrix getDevMatrix(){
        return devMatrix;
    }    

    @Override
    protected void doClose() throws IOException {
        stop();
        super.doClose();
    }
     
    @Override
    public Object read() throws IOException, InterruptedException {
        waitCacheChange(-1);
        return take();
    }
     
}
