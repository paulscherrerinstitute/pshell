package ch.psi.pshell.device;

import ch.psi.utils.Arr;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Master-slave axis .
 */
public class MasterPositioner extends PositionerBase {

    final Positioner master;
    final Positioner[] slaves;
    final MotorGroup motorGroup;

    public MasterPositioner(String name, Positioner master, Positioner... slaves) {
        super(name, new MasterPositionerConfig());
        this.master = master;
        this.slaves = slaves;
        MotorGroup motorGroup = null;
        ArrayList<Motor> motorList = new ArrayList<>();
        ArrayList<Device> components = new ArrayList<>();

        if (master instanceof Device) {
            components.add((Device) master);
        }
        for (Positioner p : slaves) {
            if (p instanceof Motor) {
                motorList.add((Motor) p);
            }
            if (p instanceof Device) {
                components.add((Device) p);
            }
        }

        if (motorList.size() > 0) {
            if (master instanceof Motor) {
                motorList.add(0, (Motor) master);
            }
            motorGroup = new MotorGroupBase(name + " motor group", motorList.toArray(new Motor[0]));
        }

        this.motorGroup = motorGroup;
        setComponents(components.toArray(new Device[0]));
        for (Device d : components) {
            d.addListener(new DeviceAdapter() {
                @Override
                public void onStateChanged(Device device, State state, State former) {
                    processState();
                }
            });
        }
    }

    public MasterPositioner(String name, Positioner master, Positioner m1) {
        this(name, master, new Positioner[]{m1});
    }

    public MasterPositioner(String name, Positioner master, Positioner m1, Positioner m2) {
        this(name, master, new Positioner[]{m1, m2});
    }

    public MasterPositioner(String name, Positioner master, Positioner m1, Positioner m2, Positioner m3) {
        this(name, master, new Positioner[]{m1, m2, m3});
    }

    public MasterPositioner(String name, Positioner master, Positioner m1, Positioner m2, Positioner m3, Positioner m4) {
        this(name, master, new Positioner[]{m1, m2, m3, m4});
    }

    public MasterPositioner(String name, Positioner master, Positioner m1, Positioner m2, Positioner m3, Positioner m4, Positioner m5) {
        this(name, master, new Positioner[]{m1, m2, m3, m4, m5});
    }

    public MasterPositioner(String name, Positioner master, Positioner m1, Positioner m2, Positioner m3, Positioner m4, Positioner m5, Positioner m6) {
        this(name, master, new Positioner[]{m1, m2, m3, m4, m5, m6});
    }

    @Override
    public MasterPositionerConfig getConfig() {
        return (MasterPositionerConfig) super.getConfig();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (master instanceof PositionerBase) {
            PositionerConfig cfg = ((PositionerBase) master).getConfig();
            getConfig().maxValue = cfg.maxValue;
            getConfig().minValue = cfg.minValue;
            getConfig().offset = cfg.offset;
            getConfig().precision = cfg.precision;
            getConfig().resolution = cfg.resolution;
            getConfig().scale = cfg.scale;
            getConfig().sign_bit = cfg.sign_bit;
            getConfig().unit = cfg.unit;
            getConfig().save();
        }
        super.doInitialize();
        if (motorGroup != null) {
            motorGroup.initialize();
        }
    }
    
    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        for (Device d : getComponents()) {
            d.update();            
        }        
    }    

    public Double[] getSlavePositions(Double masterPosition) {
        Double[] ret = new Double[slaves.length];
        for (int i = 0; i < slaves.length; i++) {
            ret[i] = masterPosition + i;
        }
        return ret;
    }

    boolean isSlaveInMotorGroup(int i) {
        return ((motorGroup != null) && Arr.contains(motorGroup.getComponents(), slaves[i]));
    }

    boolean isMasterInMotorGroup() {
        return ((motorGroup != null) && Arr.contains(motorGroup.getComponents(), master));
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        Double[] slavePositions = getSlavePositions(value);

        if (!isMasterInMotorGroup()) {
            master.moveAsync(value);
        }
        setState(State.Busy);        
        
        if (motorGroup != null) {
            double[] positions = new double[motorGroup.getMotors().length];
            int index = 0;
            if (isMasterInMotorGroup()) {
                positions[index++] = value;
            }
            for (int i = 0; i < slaves.length; i++) {
                if (isSlaveInMotorGroup(i)) {
                    positions[index++] = slavePositions[i];
                }
            }
            motorGroup.moveAsync(positions).handle((ret, ex) -> {
                processState();
                return ret;
            });
        }

        for (int i = 0; i < slaves.length; i++) {
            if (!isSlaveInMotorGroup(i)) {
                slaves[i].moveAsync(slavePositions[i]).handle((ret, ex) -> {
                    processState();
                    return ret;
                });
            }
        }        
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return master.getPosition();
    }

    @Override
    protected void processState() {
        Device[] devs = getComponents();
        for (Device dev : devs) {
            if (!dev.getState().isNormal()) {
                setState(State.Invalid);
                return;
            }
        }
        for (Device dev : devs) {
            if (dev.getState().isProcessing()) {
                setState(State.Busy);
                return;
            }
        }
        setState(State.Ready);
    }

    public Positioner getMaster() {
        return master;
    }

    public Positioner[] getSlaves() {
        return slaves;
    }

    public MotorGroup getMotorGroup() {
        return motorGroup;
    }
}
