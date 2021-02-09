package ch.psi.pshell.device;

import ch.psi.utils.Str;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interlock implementations allow to define rules regarding the state of multiple devices. When
 * device values are changing these rules are checked, and the value change is aborted with an
 * exception if an interlock rule for that device is violated. Implementations should call the base
 * class constructor with the list of controlled devices, and then override the check() method. The
 * check method receive each device value and returns true if they are allowed.
 */
public abstract class Interlock implements AutoCloseable, Record {

    final Device[] devices;
    final DeviceListener listener;

    static final ArrayList<Interlock> instances = new ArrayList();

    static final public List<Interlock> getInstances() {
        return instances;
    }

    static final public Interlock getByName(String name) {
        for (Interlock interlock : instances) {
            if (interlock.getName().equals(name)) {
                return interlock;
            }
        }
        return null;
    }

    static final public void clear() {
        for (Interlock interlock : instances.toArray(new Interlock[0])) {
            try {
                interlock.close();
            } catch (Exception ex) {
                Logger.getLogger(Interlock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        instances.clear();
    }

    boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    int moveSteps = 100;

    public int getMoveSteps() {
        return moveSteps;
    }

    /**
     * If set to 0 only motion destinations are checked, and not trajectories.
     */
    public void setMoveSteps(int value) {
        moveSteps = value;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Object[] getValues() {
        Object[] ret = new Object[devices.length];
        for (int i = 0; i < devices.length; i++) {
            ret[i] = devices[i].take();
        }
        return ret;
    }

    public void update() throws IOException, InterruptedException {
        for (Device device : devices) {
            device.update();
        }
    }

    public void request() {
        for (Device device : devices) {
            device.request();
        }
    }

    protected void setValue(Object[] values, Device device, Object value) {
        for (int i = 0; i < devices.length; i++) {
            if (device == devices[i]) {
                values[i] = value;
                break;
            }
        }
    }

    public Interlock(Device... devices) {
        for (Interlock interlock : instances) {
            if (interlock.getName().equals(getName())) {
                try {
                    interlock.close();
                } catch (Exception ex) {
                    Logger.getLogger(Interlock.class.getName()).log(Level.WARNING, null, ex);
                }
                break;
            }
        }
        this.devices = devices;
        listener = new DeviceAdapter() {
            @Override
            public void onValueChanging(Device device, Object value, Object former) throws Exception {
                if (enabled) {
                    Object[] values = getValues();
                    assertValuesOk(values);

                    if ((moveSteps > 0) && (device instanceof Positioner) && (Double.isFinite((Double) value))) {
                        Double current = (Double) device.take();
                        Double destination = (Double) value;
                        for (int i = 0; i <= moveSteps; i++) {
                            double pos = current + ((((double) i) / moveSteps) * (destination - current));
                            setValue(values, device, pos);
                            assertValid(values, device, value);
                        }
                    } else if ((moveSteps > 0) && (device instanceof MotorGroup)) {
                        double[] current = (double[]) device.take();
                        double[] destination = (double[]) value;
                        double[] pos = new double[destination.length];
                        for (int i = 0; i <= moveSteps; i++) {
                            for (int j = 0; j < destination.length; j++) {
                                if (Double.isNaN(destination[j])) {
                                    pos[j] = current[j];
                                } else if (Double.isInfinite(destination[j])) {
                                    pos[j] = destination[j];
                                } else {
                                    pos[j] = current[j] + ((((double) i) / moveSteps) * (destination[j] - current[j]));
                                }
                            }
                            setValue(values, device, pos);
                            assertValid(values, device, value);
                        }
                    } else {
                        setValue(values, device, value);
                        assertValid(values, device, value);
                    }
                }
            }

            void assertValid(Object[] values, Device device, Object setValue) throws Exception {
                if (!check(values)) {
                    throw new Exception(getName() + " interlock error: cannot set " + device.getName() + " to " + Str.toString(setValue, 10));
                }
            }

            void assertValuesOk(Object[] values) throws Exception {
                for (int i = 0; i < devices.length; i++) {
                    if (values[i] == null) {
                        throw new Exception(getName() + " interlock error: no device value cache for " + devices[i].getName());
                    }
                }
            }
        };
        instances.add(this);
        for (Device device : devices) {
            device.addListener(listener);
        }
        request();
        enabled = true;
    }

    @Override
    public void close() throws Exception {
        for (Device device : devices) {
            device.removeListener(listener);
        }
        enabled = false;
        instances.remove(this);
    }

    /**
     * Implementations should return true if the device values received are allowed. This call can
     * not be blocking or slow, as it is called repeatedly.
     */
    abstract protected boolean check(Object... value);

}
