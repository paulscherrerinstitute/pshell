package ch.psi.pshell.bs;

import ch.psi.bsread.DataChannel;
import ch.psi.bsread.Utils;
import ch.psi.bsread.common.allocator.ByteBufferAllocator;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.psi.bsread.common.helper.ByteBufferHelper;
import ch.psi.bsread.compression.Compression;
import ch.psi.bsread.converter.ByteConverter;
import ch.psi.bsread.converter.MatlabByteConverter;
import ch.psi.bsread.message.ChannelConfig;
import ch.psi.bsread.message.DataHeader;
import ch.psi.bsread.message.MainHeader;
import ch.psi.bsread.message.Timestamp;
import ch.psi.bsread.message.Type;
import ch.psi.utils.Arr;
import ch.psi.utils.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.util.function.IntFunction;
import org.zeromq.ZMQ;

/**
 * Utility class allowing encoding a set of values in the same format of a BS message.
 * Based on BSREAD java implementation.
 */
public class Encoder {

    final MainHeader mainHeader;

    final Compression dataHeaderCompression;
    final ByteConverter byteConverter;
    final ObjectMapper objectMapper;
    final IntFunction<ByteBuffer> compressedValueAllocator;
    final IntFunction<ByteBuffer> valueAllocator;

    private byte[] dataHeaderBytes;
    private String dataHeaderMD5 = "";

    private List<DataChannel<?>> channels = new ArrayList<>();

    public Encoder(ObjectMapper objectMapper) {
        this(objectMapper, Compression.none);
    }

    public Encoder(ObjectMapper objectMapper,Compression dataHeaderCompression) {
        this(objectMapper, dataHeaderCompression, new MatlabByteConverter(), ByteBufferAllocator.DEFAULT_ALLOCATOR,  ByteBufferAllocator.DEFAULT_ALLOCATOR);
    }

    public Encoder(ObjectMapper objectMapper,  Compression dataHeaderCompression, ByteConverter byteConverter, IntFunction<ByteBuffer> compressedValueAllocator, IntFunction<ByteBuffer> valueAllocator) {
        this.dataHeaderCompression = dataHeaderCompression;
        this.byteConverter = byteConverter;
        this.objectMapper = objectMapper;
        this.compressedValueAllocator = compressedValueAllocator;
        this.valueAllocator = valueAllocator;     
        mainHeader = new MainHeader(); 
    }
    
    int autoPulseId;
    public List encode() throws JsonProcessingException {
        long nanos = System.nanoTime();
        return encode(autoPulseId++, new Timestamp((long)(nanos/1e9), (long)(nanos%1e9)));
    }
    
    public List encode(long pulseId, Timestamp globalTimestamp) throws JsonProcessingException {
        DataChannel<?> channel;
        ByteOrder byteOrder;        
        
        mainHeader.setPulseId(pulseId);
        mainHeader.setGlobalTimestamp(globalTimestamp);
        mainHeader.setHash(dataHeaderMD5);
        mainHeader.setDataHeaderCompression(dataHeaderCompression);

        try {
            List ret = new ArrayList();
            ret.add(objectMapper.writeValueAsBytes(mainHeader));            
            ret.add(dataHeaderBytes);
            
            for (int i = 0; i < channels.size(); ++i) {
                channel = channels.get(i);
                byteOrder = channel.getConfig().getByteOrder();
                final Object value = channel.getValue(pulseId);

                ByteBuffer valueBuffer = byteConverter.getBytes(value, channel.getConfig().getType(), byteOrder, valueAllocator);
                valueBuffer = channel
                        .getConfig()
                        .getCompression()
                        .getCompressor()
                        .compressData(valueBuffer, valueBuffer.position(), valueBuffer.remaining(), 0,
                                compressedValueAllocator, channel.getConfig().getType().getBytes());
                ret.add(valueBuffer.array());
                
                Timestamp timestamp = channel.getTime(pulseId);
                ByteBuffer timeBuffer = byteConverter.getBytes(timestamp.getAsLongArray(), Type.Int64, byteOrder, valueAllocator);
                ret.add(timeBuffer.array());                                
            }
            return ret;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize message", e);
        }
    }

    private void generateDataHeader() {
        DataHeader dataHeader = new DataHeader();

        for (DataChannel<?> channel : channels) {
            dataHeader.addChannel(channel.getConfig());
        }

        try {
            dataHeaderBytes = objectMapper.writeValueAsBytes(dataHeader);
            if (!Compression.none.equals(dataHeaderCompression)) {
                ByteBuffer tmpBuf = dataHeaderCompression.getCompressor().compressDataHeader(ByteBuffer.wrap(dataHeaderBytes),
                        compressedValueAllocator);
                dataHeaderBytes = ByteBufferHelper.copyToByteArray(tmpBuf);
            }
            dataHeaderMD5 = Utils.computeMD5(dataHeaderBytes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to generate data header", e);
        }
    }

    public void addChannel(DataChannel<?> channel) {
        channels.add(channel);
        generateDataHeader();
    }

    public void removeChannel(DataChannel<?> channel) {
        channels.remove(channel);
        generateDataHeader();
    }
    
    
    public DataChannel addValue(String name, Object data){
        return addValue(name, data, Compression.none);
    }
    
    public DataChannel addValue(String name, Object data, boolean unsigned){
        return addValue(name, data, unsigned, Compression.none);
    }

    public DataChannel addValue(String name, Object data, Compression compression){
        return addValue(name, data, false, Compression.none);
    }

    public DataChannel addValue(String name, Object data, boolean unsigned, Compression compression){      
        Class cls = data.getClass();        
        Type type = classToType(cls, unsigned);
        int[] shape = {1};
        if (cls.isArray()){
            shape = Arr.getShape(data);                
        }
        int numberDimensions = shape.length;

        DataChannel channel = new DataChannel(new ChannelConfig(name, type, shape, 1, 0, ChannelConfig.DEFAULT_ENCODING, compression)) {
            @Override
            public Object getValue(long pulseId) {
                if (numberDimensions>1){
                    return Convert.flatten(data);
                }
                return data;
            }
        };            
        
        addChannel(channel);        
        return channel;
    }

    public static Type classToType(Class cls, boolean unsigned){
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }        
        if (cls.isPrimitive()){
            cls = Convert.getWrapperClass(cls);
        }
        if (cls == Double.class){
            return  Type.Float64;
        }
        if (cls == Float.class){
            return  Type.Float32;
        }        
        if (cls == Byte.class){
            return Type.Int8;
        }
        if (cls == Short.class){
            return unsigned ? Type.UInt8 : Type.Int16;
        }
        if (cls == Integer.class){
            return unsigned ? Type.UInt16 : Type.Int32;
        }
        if (cls == Long.class){
            return unsigned ? Type.UInt32 : Type.Int64;
        }
        if (cls == BigInteger.class){
            return unsigned ? Type.UInt64 : Type.Int64;
        }
        if (cls == String.class){
            return Type.String;
        }
        if (cls == Boolean.class){
            return Type.Bool;
        }
        throw new IllegalArgumentException("Invalid class: " + cls);
    }

    public List<DataChannel<?>> getChannels() {
        return Collections.unmodifiableList(channels);
    }            
    
    static public List decode(List message) throws IOException{
        List ret = new ArrayList();
        ByteConverter byteConverter = new MatlabByteConverter();
        ObjectMapper mapper = new ObjectMapper();
        int index=0;
        MainHeader mainHeader = mapper.readValue((byte[])message.get(index++), MainHeader.class);              
        DataHeader dataHeader = mapper.readValue((byte[])message.get(index++), DataHeader.class);        
        
        ChannelConfig timestampConfig = new ChannelConfig();
        timestampConfig.setType(Type.Int64);
        timestampConfig.setShape(new int[]{2});
        
        for (ChannelConfig channelConfig : dataHeader.getChannels()) {
            ByteBuffer valueBuffer = ByteBuffer.wrap((byte[])message.get(index++));  
            Object data = byteConverter.getValue(mainHeader, dataHeader, channelConfig, valueBuffer, mainHeader.getGlobalTimestamp());
            
            if (channelConfig.getShape().length >1){
                data = Convert.reshape(data, channelConfig.getShape());
            }
            ret.add(data);
            
            timestampConfig.setByteOrder(channelConfig.getByteOrder());                                           
            ByteBuffer timestampBuffer = ByteBuffer.wrap((byte[])message.get(index++));           
            long[] timestampArr = byteConverter.getValue(mainHeader, dataHeader, timestampConfig, timestampBuffer, mainHeader.getGlobalTimestamp());
            Timestamp timestamp = new Timestamp(timestampArr[0], timestampArr[1]);            
        }        
        return ret;
    }
    
    static public List receive(ZMQ.Socket socket) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        List ret = new ArrayList();
        ret.add(socket.recv());
        byte[] dataHeaderBytes = socket.recv();
        DataHeader dataHeader = mapper.readValue(dataHeaderBytes, DataHeader.class);        
        ret.add(dataHeaderBytes);
        for (ChannelConfig channelConfig : dataHeader.getChannels()) {
            ret.add(socket.recv());
            ret.add(socket.recv());
        }        
        return decode(ret);
    }
    
}
