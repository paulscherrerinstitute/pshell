package ch.psi.pshell.epics;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.utils.State;
import java.io.IOException;

/**
 *
 */
public class PsiCamera extends ArraySource {

    final String prefix;
    final ChannelString channelStatus;

    public static class PsiCameraConfig extends RegisterArraySourceConfig {

        public int regionStartX;
        public int regionStartY;
    }

    @Override
    public PsiCameraConfig getConfig() {
        return (PsiCameraConfig) super.getConfig();
    }

    public PsiCamera(String name, String prefix) {
        super(name, prefix + ":FPICTURE", new PsiCameraConfig());
        this.prefix = prefix;
        channelStatus = new ChannelString(name + " status", prefix + ":INIT");
        channelStatus.addListener(new DeviceAdapter() {
            @Override
            public void onValueChanged(Device device, Object value, Object former) {
                setState("INIT".equals(channelStatus.take()) ? State.Ready : State.Offline);
            }
        });
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        channelStatus.update();
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        channelStatus.setMonitored(value);
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        try {
            getConfig().imageHeight = Epics.get(prefix + ":HEIGHT", Integer.class);
            getConfig().imageWidth = Epics.get(prefix + ":WIDTH", Integer.class);
            getConfig().regionStartX = Epics.get(prefix + ":REGIONX_START", Integer.class);
            getConfig().regionStartY = Epics.get(prefix + ":REGIONY_START", Integer.class);
            getConfig().save();
            getDevice().setSize(getConfig().imageHeight * getConfig().imageWidth);
            channelStatus.initialize();
            channelStatus.update();
            super.doInitialize();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
}
