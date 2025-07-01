package ch.psi.pshell.epics;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.pshell.device.ReadonlyRegister;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import ch.psi.pshell.device.RegisterConfig;
import ch.psi.pshell.utils.Convert;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Wraps an EPICS PV as register matrix with variable type.
 */
public class GenericMatrix extends ReadonlyRegisterBase implements ReadonlyRegister.ReadonlyRegisterMatrix {

    final GenericArray array;
    final int width;
    final int height;

    public static class GenericMatrixConfig extends RegisterConfig {

        public boolean transpose = false;
        public boolean mirror_x = false;
        public boolean mirror_y = false;
        public int roi_x = 0;
        public int roi_y = 0;
        public int roi_width = UNDEFINED;
        public int roi_height = UNDEFINED;
    }

    @Override
    public GenericMatrixConfig getConfig() {
        return (GenericMatrixConfig) super.getConfig();
    }

    public GenericMatrix(String name, String channelName, int width, int height) {
        this(name, channelName, width, height, true);
    }

    public GenericMatrix(String name, String channelName, int width, int height, boolean timestamped) {
        this(name, channelName, width, height, timestamped, timestamped ? Epics.getDefaultInvalidValueAction(): null);
    }

    public GenericMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction) {
        this(name, channelName, width, height, timestamped, invalidAction, null);
    }

    
    public GenericMatrix(String name, String channelName, int width, int height, boolean timestamped, InvalidValueAction invalidAction, String type) {
        super(name, new GenericMatrixConfig());
        array = new GenericArray(name + " array", channelName, width * height, timestamped, invalidAction, type);
        this.width = width;
        this.height = height;
        array.addListener(new DeviceListener() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                try {
                    setCache(process(value));
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        });

        addChild(array);
    }

    @Override
    protected Object doRead() throws IOException, InterruptedException {
        return process(array.read());
    }

    Object process(Object data) {
        if (data == null) {
            return data;
        }
        Object matrix = Convert.reshape(data, height, width);
        matrix = Convert.matrixOp(matrix, getConfig().transpose, getConfig().mirror_x, getConfig().mirror_y);
        matrix = Convert.matrixRoi(matrix, getConfig().roi_x, getConfig().roi_y, getConfig().roi_width, getConfig().roi_height);
        return matrix;
    }

    @Override
    public int getWidth() {
        int w = getConfig().transpose ? height : width;
        int pos = Math.min(Math.max(getConfig().roi_x, 0), w);
        int roi = getConfig().roi_width < 0 ? w : getConfig().roi_width;
        return Math.min(roi, w - pos);
    }

    @Override
    public int getHeight() {
        int h = getConfig().transpose ? width : height;
        int pos = Math.min(Math.max(getConfig().roi_y, 0), h);
        int roi = getConfig().roi_height < 0 ? h : getConfig().roi_height;
        return Math.min(roi, h - pos);
    }
}
