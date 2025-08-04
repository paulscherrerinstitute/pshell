package ch.psi.pshell.imaging;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.pshell.devices.DevicePool;
import ch.psi.pshell.devices.DevicePoolListener;

/**
 */
public class DeviceRenderer extends Renderer {
    //Fixed source name
    private String deviceName;

    public void setDeviceName(String value) {
        deviceName = value;
        if (devicePoolListener != null) {
            DevicePool.removeStaticListener(devicePoolListener);
            devicePoolListener = null;
        }       
        if ((deviceName != null) && (!deviceName.isEmpty())) {
            devicePoolListener = new DevicePoolListener() {
                @Override
                public void onInitialized() {
                    setDevice(DevicePool.getInstance().getByName(deviceName, Source.class));
                }
                
                @Override
                public  void onClosing() {
                    setDevice(null);
                }
                
                @Override
                public void onDeviceAdded(GenericDevice dev) {
                    if (dev instanceof Source source){
                        if (deviceName.equals(source.getName())){
                            if (DevicePool.getInstance().isInitialized()){
                                onInitialized();
                            }
                        }                            
                    }
                }

                @Override
                public  void onDeviceRemoved(GenericDevice dev) {
                 if (dev instanceof Source source){
                        if (deviceName.equals(source.getName())){
                            onClosing();
                        }                            
                    }                    
                }                
            };
            DevicePool.addStaticListener(devicePoolListener);
            if (DevicePool.hasInstance()){
                devicePoolListener.onInitialized();           
            }
        }
    }

    DevicePoolListener devicePoolListener;

    @Override
    protected void onDesactive() {
        if (devicePoolListener != null) {
            DevicePool.removeStaticListener(devicePoolListener);
            devicePoolListener = null;
        }
    }

    public String getDeviceName() {
        if (deviceName == null) {
            return "";
        }
        return deviceName;
    }

    Source device;

    public void setDevice(final Source device) {
        if (this.device != null) {
            this.device.removeListener(this);
        }
        this.device = device;
        if (device != null) {
            if (this.isShowing()) {
                device.addListener(this);
            }
            device.refresh();
        }
    }

    public Source getDevice() {
        return device;
    }

    @Override
    protected void onShow() {
        if (device != null) {
            device.addListener(this);
            device.refresh();
        }
        super.onShow();
    }

    @Override
    protected void onHide() {
        if (device != null) {
            device.removeListener(this);
        }
        super.onHide();
    }    
    
}
