package ch.psi.pshell.imaging;

import ch.psi.pshell.device.Readable.ReadableArray;
import ch.psi.pshell.device.ReadonlyRegister.ReadonlyRegisterArray;
import ch.psi.pshell.device.ReadonlyRegisterBase;
import java.io.IOException;

/**
 * Image source connecting to a __ReadableArray__, pushing a frame every time the register contents
 * change.
 */
public class RegisterArraySource extends DeviceSource {

    public static class RegisterArraySourceConfig extends ColormapSourceConfig {

        public int imageWidth;
        public int imageHeight;
    }

    @Override
    public RegisterArraySourceConfig getConfig() {
        return (RegisterArraySourceConfig) super.getConfig();
    }

    public RegisterArraySource(String name, ReadonlyRegisterArray device) {
        this(name, device, null);
    }

    protected RegisterArraySource(String name, ReadonlyRegisterArray device, RegisterArraySourceConfig config) {
        super(name, device, (config == null) ? new RegisterArraySourceConfig() : config);
    }

    //Wrapper constructor to readable
    public RegisterArraySource(String name, ReadableArray readable) {
        this(name, new WrapperRegister(readable));
    }

    static class WrapperRegister extends ReadonlyRegisterBase implements ReadonlyRegisterArray {

        final ReadableArray readable;

        WrapperRegister(ReadableArray readable) {
            super(readable.getName());
            this.readable = readable;
            try {
                initialize();
            } catch (Exception ignore) {
            }
        }

        @Override
        protected Object doRead() throws IOException, InterruptedException {
            return readable.read();
        }

        @Override
        public int getSize() {
            return readable.getSize();
        }
    }

    boolean unsigned = true;

    @Override
    protected void onDataReceived(Object value) throws IOException {
        if (value == null) {
            pushImage(null);
        } else {
            pushData(value, getConfig().imageWidth, getConfig().imageHeight);
        }
    }
}
