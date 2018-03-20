package ch.psi.pshell.epics;

import ch.psi.pshell.device.CameraBase;
import ch.psi.pshell.device.CameraImageDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.Register.RegisterArray;
import ch.psi.pshell.device.AccessType;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Wraps EPICS AreaDetector, implementing Camera.
 */
public class AreaDetector extends CameraBase {

    final GenericArray data;
    final ChannelInteger acquire;
    final ChannelString colorMode, dataType;
    final ChannelInteger arraySize0, arraySize1, arraySize2;
    final ChannelInteger minX, minY, sizeX, sizeY;
    final String channelCtrl, channelData;
    final ChannelInteger imageCounter;

    public AreaDetector(final String name, final String channelPrefix) {
        this(name, channelPrefix + ":cam1", channelPrefix + ":image1");
    }

    public AreaDetector(final String name, final String channelCtrl, final String channelData) {
        super(name);
        this.channelCtrl = channelCtrl;
        this.channelData = channelData;
        acquire = new ChannelInteger(name + " acquire", channelCtrl + ":Acquire", false);
        //imageSizeX = new ChannelInteger(name + " size x rdb", channelCtrl + ":ArraySizeX_RBV", false);
        //imageSizeY = new ChannelInteger(name + " size y rdb", channelCtrl + ":ArraySizeY_RBV", false);
        arraySize0 = new ChannelInteger(name + " array size 0", channelData + ":ArraySize0_RBV", false);
        arraySize1 = new ChannelInteger(name + " array size 1", channelData + ":ArraySize1_RBV", false);
        arraySize2 = new ChannelInteger(name + " array size 2", channelData + ":ArraySize2_RBV", false);
        colorMode = new ChannelString(name + " color mode", channelCtrl + ":ColorMode", false);
        dataType = new ChannelString(name + " data type", channelCtrl + ":DataType", false);
        minX = new ChannelInteger(name + " min x", channelCtrl + ":MinX", false);
        minY = new ChannelInteger(name + " min x", channelCtrl + ":MinY", false);
        sizeX = new ChannelInteger(name + " size x", channelCtrl + ":SizeX", false);
        sizeY = new ChannelInteger(name + " size y", channelCtrl + ":SizeY", false);
        imageCounter = new ChannelInteger(name + " img counter", channelCtrl + ":ArrayCounter_RBV", false);
        imageCounter.setAccessType(AccessType.Read);
        data = new GenericArray(name + " data", channelData + ":ArrayData");
        data.setAutoResolveType(false);

        this.setChildren(new Device[]{data, imageCounter, acquire, arraySize0, arraySize1, arraySize2, colorMode, dataType, minX, minY, sizeX, sizeY});
        acquire.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                boolean started = (value != null) && ((Integer) value > 0);
                updateState(started);
            }
        });

        imageCounter.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                synchronized (imageCounter) {
                    imageCounter.notifyAll();
                }
            }
        });

        DeviceListener imageFormatListener = new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                try {
                    CameraImageDescriptor desc = readImageDescriptor();
                    if (device == dataType) {
                        data.setType(DataType.valueOf((String) value).getArrayType());
                    }
                    data.setSize(desc.getTotalSize());
                } catch (Exception ex) {
                    getLogger().log(Level.FINEST, null, ex);
                }
            }
        };
        arraySize0.addListener(imageFormatListener);
        arraySize1.addListener(imageFormatListener);
        arraySize2.addListener(imageFormatListener);
        colorMode.addListener(imageFormatListener);
        dataType.addListener(imageFormatListener);
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        int width = 200;
        int height = 100;
        setCache(acquire, 1);
        setCache(arraySize0, width);
        setCache(arraySize1, height);
        setCache(arraySize2, 1);
        setCache(colorMode, ColorMode.Mono.toString());
        setCache(dataType, DataType.UInt8.toString());
        setCache(minX, 0);
        setCache(minY, 0);
        setCache(sizeX, width);
        setCache(sizeY, height);

        setSimulatedValue("ImageMode", GrabMode.Continuous.toString());
        setSimulatedValue("TriggerMode", TriggerMode.Internal.toString());
        setSimulatedValue("BinX", 1);
        setSimulatedValue("BinY", 1);
        setSimulatedValue("NumExposures", 1);
        setSimulatedValue("Gain", 1.0);
        setSimulatedValue("AcquireTime", 0.01);
        setSimulatedValue("AcquirePeriod", 0.05);
        setSimulatedValue("MaxSizeX_RBV", width * 5);
        setSimulatedValue("MaxSizeY_RBV", height * 5);
        setSimulatedValue("NumImagesCounter_RBV", 0);
        setSimulatedValue("NumImages", 0);
        setSimulatedValue("Manufacturer_RBV", "AreaDetector");
        setSimulatedValue("Model_RBV", "Simulation");

        try {
            data.setType(byte[].class);
            data.setSize(width * height);
            byte[] array = new byte[data.getSize()];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    array[j * width + i] = (byte) (i / 2 + j / 2);
                }
            }
            this.setCache(data, array);
        } catch (Exception ex) {

        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        dataType.resetCache();
        super.doInitialize();
        doUpdate();
        data.initialize();
    }

    @Override
    public RegisterArray getDataArray() {
        return data;
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        acquire.update();
        arraySize0.update();
        arraySize1.update();
        arraySize2.update();
        colorMode.update();
        dataType.update();
        imageCounter.update();
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        acquire.setMonitored(value);
        arraySize0.setMonitored(value);
        arraySize1.setMonitored(value);
        arraySize2.setMonitored(value);
        colorMode.setMonitored(value);
        dataType.setMonitored(value);
        imageCounter.setMonitored(value);
    }

    public int getCurrentImageCount() throws InterruptedException, DeviceTimeoutException {
        Integer ret = imageCounter.take();
        if (ret == null) {
            return Integer.MIN_VALUE;
        }
        return ret;
    }

    public void waitNewImage(int timeout) throws InterruptedException, DeviceTimeoutException {
        waitNewImage(timeout, getCurrentImageCount());
    }

    public void waitNewImage(int timeout, int current) throws InterruptedException, DeviceTimeoutException {

        if (current != getCurrentImageCount()) {
            return;
        }

        if (timeout < 0) {
            timeout = 0;
        }

        synchronized (imageCounter) {
            imageCounter.wait(timeout);
        }

        if (current == getCurrentImageCount()) {
            throw new DeviceTimeoutException();
        }
    }

    protected Object readCtrl(String field, Class type) throws IOException, InterruptedException {
        try {
            if (isSimulated()) {
                return getSimulatedValue(field);
            } else {
                return Epics.get(channelCtrl + ":" + field, type);
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex);
        }
    }

    protected void writeCtrl(String field, Object value) throws IOException, InterruptedException {
        assertWriteEnabled();
        try {
            if (isSimulated()) {
                setSimulatedValue(field, value);
            } else {
                Epics.putq(channelCtrl + ":" + field, value);
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex);
        }
    }

    protected Object readCtrlEnum(String field, Class cls) throws IOException, InterruptedException {
        String val = (String) readCtrl(field, String.class);
        return convertCtrlEnum(val, cls);
    }

    protected Object convertCtrlEnum(String val, Class cls) throws IOException, InterruptedException {
        if ((val == null) || (cls == null) || (!cls.isEnum())) {
            if ((cls != null) && isSimulated()) {
                Object[] vals = cls.getEnumConstants();
                if ((vals != null) && (vals.length > 0)) {
                    return vals[0];
                }
            }
            return null;
        }
        try {
            val = val.trim().replaceAll(" ", "_");
            return Enum.valueOf(cls, val);
        } catch (Exception ex) {
            //try {                
            //    return cls.getEnumConstants()[Integer.valueOf(val)]; //Check if return is an index
            //} catch (Exception e){
            throw new DeviceInvalidParameterException(cls.getSimpleName(), val);
            //}
        }
    }

    protected void writeCtrlEnum(String field, Object value) throws IOException, InterruptedException {
        String val = String.valueOf(value).replaceAll("_", " ");
        writeCtrl(field, val);
    }

    @Override
    protected void doStart() throws IOException, InterruptedException {
        acquire.write(1);
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        acquire.write(0);
    }

    @Override
    protected boolean getStarted() throws IOException, InterruptedException {
        return acquire.getValue() > 0;
    }

    @Override
    public int[] getSensorSize() throws IOException, InterruptedException {
        return new int[]{(Integer) readCtrl("MaxSizeX_RBV", Integer.class),
            (Integer) readCtrl("MaxSizeY_RBV", Integer.class)
        };
    }

    @Override
    public void setBinningX(int value) throws IOException, InterruptedException {
        writeCtrl("BinX", value);
        request();
    }

    @Override
    public int getBinningX() throws IOException, InterruptedException {
        return (Integer) readCtrl("BinX", Integer.class);
    }

    @Override
    public void setBinningY(int value) throws IOException, InterruptedException {
        writeCtrl("BinY", value);
        request();
    }

    @Override
    public int getBinningY() throws IOException, InterruptedException {
        return (Integer) readCtrl("BinY", Integer.class);
    }

    @Override
    public void setROI(int x, int y, int w, int h) throws IOException, InterruptedException {
        if (x >= 0) {
            minX.write(x);
        }
        if (y >= 0) {
            minY.write(y);
        }
        if (w >= 0) {
            sizeX.write(w);
        }
        if (h >= 0) {
            sizeY.write(h);
        }
        request();
    }

    @Override
    public int[] getROI() throws IOException, InterruptedException {
        return new int[]{minX.read(), minY.read(), sizeX.read(), sizeY.read()};
    }

    @Override
    public int[] getImageSize() throws IOException, InterruptedException {
        ColorMode mode = takeColorMode();
        if (mode == null) {
            mode = getColorMode();
        }
        Integer size0 = arraySize0.getValue();
        if (size0 == null) {
            size0 = 0;
        }
        Integer size1 = arraySize1.getValue();
        if (size1 == null) {
            size1 = 0;
        }

        switch (mode) {
            case Mono:
                return new int[]{size0, size1};
            case RGB1: {
                Integer size2 = arraySize2.getValue();
                if (size2 == null) {
                    size2 = 0;
                }
                return new int[]{size1, size2};
            }
            case RGB2: {
                Integer size2 = arraySize2.getValue();
                if (size2 == null) {
                    size2 = 0;
                }
                return new int[]{size0, size2};
            }
            case RGB3:
            default:
                return new int[]{size0, size1};
        }
    }

    @Override
    public void setGain(double value) throws IOException, InterruptedException {
        writeCtrl("Gain", value);
    }

    @Override
    public double getGain() throws IOException, InterruptedException {
        return (Double) readCtrl("Gain", Double.class);
    }

    @Override
    public void setDataType(DataType type) throws IOException, InterruptedException {
        if (type == null) {
            throw new DeviceInvalidParameterException();
        }
        dataType.write(type.toString());
    }

    @Override
    public DataType getDataType() throws IOException, InterruptedException {
        String type = dataType.getValue();
        if (type == null) {
            return null;
        }
        try {
            return DataType.valueOf(type);
        } catch (Exception ex) {
            throw new DeviceInvalidParameterException("DataType", type);
        }
    }

    DataType takeDataType() throws IOException, InterruptedException {
        String type = dataType.take();
        if (type == null) {
            return null;
        }
        try {
            return DataType.valueOf(type);
        } catch (Exception ex) {
            throw new DeviceInvalidParameterException("DataType", type);
        }
    }

    @Override
    public void setColorMode(ColorMode mode) throws IOException, InterruptedException {
        if (mode == null) {
            throw new DeviceInvalidParameterException();
        }
        colorMode.write(mode.toString());
    }

    @Override
    public ColorMode getColorMode() throws IOException, InterruptedException {
        String mode = colorMode.getValue();
        if (mode == null) {
            return ColorMode.Mono;
        }
        try {
            return ColorMode.valueOf(mode);
        } catch (Exception ex) {
            throw new DeviceInvalidParameterException("ColorMode", mode);
        }
    }

    ColorMode takeColorMode() throws IOException, InterruptedException {
        String mode = colorMode.take();
        if (mode == null) {
            return null;
        }
        try {
            return ColorMode.valueOf(mode);
        } catch (Exception ex) {
            throw new DeviceInvalidParameterException("ColorMode", mode);
        }
    }

    protected String getManufacturer() throws IOException, InterruptedException {
        return (String) readCtrl("Manufacturer_RBV", String.class);
    }

    protected String getModel() throws IOException, InterruptedException {
        return (String) readCtrl("Model_RBV", String.class);
    }

    @Override
    public String getInfo() throws IOException, InterruptedException {
        return getManufacturer() + " - " + getModel();
    }

    @Override
    public void setExposure(double value) throws IOException, InterruptedException {
        writeCtrl("AcquireTime", value);
    }

    @Override
    public double getExposure() throws IOException, InterruptedException {
        return (Double) readCtrl("AcquireTime", Double.class);
    }

    @Override
    public void setAcquirePeriod(double value) throws IOException, InterruptedException {
        writeCtrl("AcquirePeriod", value);
    }

    @Override
    public double getAcquirePeriod() throws IOException, InterruptedException {
        return (Double) readCtrl("AcquirePeriod", Double.class);
    }

    @Override
    public void setNumImages(int value) throws IOException, InterruptedException {
        writeCtrl("NumImages", value);
    }

    @Override
    public int getNumImages() throws IOException, InterruptedException {
        return (Integer) readCtrl("NumImages", Integer.class);
    }

    @Override
    public void setIterations(int value) throws IOException, InterruptedException {
        writeCtrl("NumExposures", value);
    }

    @Override
    public int getIterations() throws IOException, InterruptedException {
        return (Integer) readCtrl("NumExposures", Integer.class);
    }

    @Override
    public int getImagesComplete() throws IOException, InterruptedException {
        return (Integer) readCtrl("NumImagesCounter_RBV", Integer.class);
    }

    @Override
    public void setGrabMode(GrabMode value) throws IOException, InterruptedException {
        writeCtrlEnum("ImageMode", value.toString());
    }

    @Override
    public GrabMode getGrabMode() throws IOException, InterruptedException {
        return (GrabMode) readCtrlEnum("ImageMode", GrabMode.class);
    }

    @Override
    public void setTriggerMode(TriggerMode value) throws IOException, InterruptedException {
        String str = value.toString();
        if (value.isExternal()) {
            switch (getManufacturer()) {
                case "Prosilica":
                    switch (value) {
                        case External:
                            str = "Sync In 1";
                            break;
                        case External_2:
                            str = "Sync In 2";
                            break;
                        case External_3:
                            str = "Sync In 3";
                            break;
                        case External_4:
                            str = "Sync In 4";
                            break;
                    }
                    break;
                default:
                    str = TriggerMode.External.toString();
            }
        }
        writeCtrlEnum("TriggerMode", str);
    }

    @Override
    public TriggerMode getTriggerMode() throws IOException, InterruptedException {
        String val = (String) readCtrl("TriggerMode", String.class);
        try {
            return (TriggerMode) convertCtrlEnum(val, TriggerMode.class);
        } catch (DeviceInvalidParameterException ex) {
            switch (getManufacturer()) {
                case "Prosilica":
                    switch (val) {
                        case "Sync In 1":
                            return TriggerMode.External;
                        case "Sync In 2":
                            return TriggerMode.External_2;
                        case "Sync In 3":
                            return TriggerMode.External_2;
                        case "Sync In 4":
                            return TriggerMode.External_2;
                    }
            }
            throw ex;
        }
    }

    public void setCallbacksEnabled(boolean value) throws IOException, InterruptedException {
        writeCtrl("ArrayCallbacks", value ? 1 : 0);
    }

    public boolean getCallbacksEnabled() throws IOException, InterruptedException {
        return ((Integer) readCtrl("ArrayCallbacks", Integer.class)) > 0;
    }

    //Direct register access
    public ChannelInteger getArraySize0() {
        return arraySize0;
    }

    public ChannelInteger getArraySize1() {
        return arraySize1;
    }

    public ChannelInteger getArraySize2() {
        return arraySize2;
    }

    public ChannelInteger getMinX() {
        return minX;
    }

    public ChannelInteger getMinY() {
        return minX;
    }

    public ChannelInteger getSizeX() {
        return sizeX;
    }

    public ChannelInteger getSizeY() {
        return sizeY;
    }

    public ChannelInteger getAcquire() {
        return acquire;
    }

    public ChannelInteger getImageCounter() {
        return imageCounter;
    }

    //TODO: Shutter
    //TODO: Separate ArrayDetector and Camera classes
    @Override
    public void trigger() throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
