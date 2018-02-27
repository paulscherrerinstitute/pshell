package ch.psi.pshell.scan;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceBase;
import ch.psi.pshell.device.DeviceListener;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CompositeTrigger extends DeviceBase {
    
    final Device[] triggers;
    final DeviceListener listener;
    final Map values;

    public CompositeTrigger(Device[] triggers) {
        this.triggers = triggers;
        values = new HashMap();
        listener = new DeviceAdapter() {
            @Override
            public void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange) {
                Object dev = String.valueOf(device.getName());
                values.put(dev, value);
                CompositeTrigger.this.setCache(values);
            }
        };
    }

    @Override
    public void doInitialize() {
        for (Device trigger : triggers) {
            trigger.addListener(listener);
        }
    }

    @Override
    protected void doClose() {
        for (Device trigger : triggers) {
            trigger.removeListener(listener);
        }
    }
    
}
