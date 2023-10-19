package ch.psi.pshell.bs;

import java.io.IOException;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.bsread.ReceiverConfig;
import ch.psi.bsread.allocator.HeaderReservingMsgAllocator;
import ch.psi.bsread.common.allocator.ByteBufferAllocator;
import ch.psi.bsread.converter.MatlabByteConverter;
import ch.psi.bsread.impl.StandardMessageExtractor;
import ch.psi.pshell.bs.ProviderConfig.SocketType;
import ch.psi.pshell.core.Context;
import org.zeromq.ZMQ;
import zmq.msg.MsgAllocator;

/**
 * Device implementing the provider of a BS stream.
 */
public class Provider extends DeviceBase {

    final ProviderConfig volatileConfig;
    String address;
    public static final int MAX_HEADER_BYTES = Short.BYTES +      // ID 
                                               Byte.BYTES +       // Compression algo 
                                               Byte.BYTES +       // Dim 
                                               2 * Integer.BYTES; // Shape elements, max 2d
    
    //Persisted configuration
    public Provider(String name, String address) {
        this(name, address, new ProviderConfig());
    }
    
    public Provider(String name, String address, ProviderConfig config) {
        super(name, config);
        this.address = address;
        this.volatileConfig = null;
    }    

    //Dynamic configuration
    public Provider(String name, String address, boolean pull) {
        this(name, address, pull ? SocketType.PULL : SocketType.SUB);
    }
    
    public Provider(String name, String address, SocketType socketType) {
        super(name);
        this.address = address;
        volatileConfig = new ProviderConfig();
        volatileConfig.socketType =  socketType;
    }    

    @Override
    public ProviderConfig getConfig() {
        if (volatileConfig != null) {
            return volatileConfig;
        }
        return (ProviderConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStreamSocket(Stream stream) {
        return getAddress();
    }

    public int getSocketType() {
        return (getConfig().socketType == SocketType.PULL) ? ZMQ.PULL : ZMQ.SUB;
    }
    
    public boolean getAnalizeHeader() {
        return Boolean.TRUE.equals(getConfig().analizeHeader) ? true : false; 
    }    

    public boolean setAnalizeHeader(boolean value) {
        return getConfig().analizeHeader = value;
    }    

    
    public ReceiverConfig getReceiverConfig(Stream stream) throws Exception {
        String socket = getStreamSocket(stream);
        if (socket == null) {
            throw new Exception("Stream socket is not defined");
        }
        MsgAllocator allocator = getConfig().headerReservingAllocator ? new HeaderReservingMsgAllocator(MAX_HEADER_BYTES, ByteBufferAllocator.DEFAULT_ALLOCATOR) : null;
        ReceiverConfig ret = new ReceiverConfig(socket,true, false, new StandardMessageExtractor<>(new MatlabByteConverter()),allocator);
        ret.setSocketType(getSocketType());
        ret.setKeepListeningOnStop(getConfig().keepListeningOnStop);
        ret.setParallelHandlerProcessing(getConfig().parallelHandlerProcessing);
        return ret;
    }

    protected void createStream(Stream stream) throws IOException {
    }

    protected void closeStream(Stream stream) {
    }
    
    public static Provider getDefault() {
        if (Context.getInstance()==null){
            return null;
        }
        return Context.getInstance().getDevicePool().getByName("dispatcher", ch.psi.pshell.bs.Provider.class);
    }
    
    public static Provider getOrCreateDefault() {
        Provider dispatcher = getDefault();
        if (dispatcher == null) {
            dispatcher = Dispatcher.createDefault();
        }
        return dispatcher;
    }              
}
