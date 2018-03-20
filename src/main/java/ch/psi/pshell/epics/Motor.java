package ch.psi.pshell.epics;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.MotorConfig;
import ch.psi.pshell.device.MotorBase;
import ch.psi.pshell.device.MotorStatus;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.DeviceAdapter;
import ch.psi.pshell.device.DeviceListener;
import ch.psi.utils.BitMask;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Wraps EPICS motor records.
 */
public class Motor extends MotorBase {

    final String channelName;
    final ChannelDouble setpoint;
    final ChannelDouble readback;
    final ChannelDouble velocity;
    final ChannelString mode;
    final ChannelInteger enabled;
    final ChannelInteger stop;
    final ChannelInteger state;
    final ChannelShort done;

    double version;

    public enum HomingType {

        None,
        Backward,
        Forward
    }

    public static class EpicsMotorConfig extends MotorConfig {

        public HomingType homingType = HomingType.None;
        public boolean hasEnable = false;
    }

    //TODO: Check why some motors have enabled channelName
    public Motor(String name, String channelName) {
        super(name, new EpicsMotorConfig());
        this.channelName = channelName;
        setpoint = new ChannelDouble(name + " setpoint", this.channelName + ".VAL");
        velocity = new ChannelDouble(name + " speed", this.channelName + ".VELO", -1, false);
        readback = new ReadbackChannel(name + " readback", this.channelName + ".RBV");
        readback.setAccessType(AccessType.Read);
        state = new ChannelInteger(name + " state", this.channelName + ".MSTA", false);
        state.setAccessType(AccessType.Read);
        stop = new ChannelInteger(name + " stop", this.channelName + ".STOP", false);
        mode = new ChannelString(name + " mode", this.channelName + ".SPMG", false);
        done = new ChannelShort(name + " done", this.channelName + ".DMOV", false);
        if (getConfig().hasEnable) {
            enabled = new ChannelInteger(name + " enabled", this.channelName + "_able.VAL", false);
            enabled.setAccessType(AccessType.Read);
        } else {
            enabled = null;
        }

        setChildren(new Device[]{setpoint, readback, velocity, mode, state, stop, done, enabled});
        setReadback(readback);
        setVelocity(velocity);
    }

    @Override
    public EpicsMotorConfig getConfig() {
        return (EpicsMotorConfig) super.getConfig();
    }

    public String getChannelName() {
        return channelName;
    }

    public ChannelDouble getSetpoint() {
        return setpoint;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();

        //If units not set assumes it is first execution and uploads config from motor record
        if (!getConfig().hasDefinedUnit()) {
            uploadConfig();
        }

        setMonitored(isMonitored());
        try {
            if (!isSimulated()) {
                version = Epics.get(channelName + ".VERS", Double.class);
                version = Math.floor(version * 10) / 10;
            }
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Cannot read version: " + ex.getMessage());
            version = 0.0;
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        for (Device dev : getChildren()) {
            if (dev instanceof EpicsRegister) {
                if (dev.isMonitored() != value) {
                    dev.setMonitored(value);
                }
                if (value) {
                    dev.addListener(changeListener);
                } else {
                    dev.removeListener(changeListener);
                }
            }
        }
    }

    public void uploadConfig() throws IOException, InterruptedException {
        if (isSimulated()) {
            return;
        }
        EpicsMotorConfig cfg = getConfig();
        try {
            cfg.minValue = Epics.get(channelName + ".LLM", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.maxValue = Epics.get(channelName + ".HLM", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.defaultSpeed = Epics.get(channelName + ".VELO", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.maxSpeed = Epics.get(channelName + ".VMAX", Double.class);
            if (cfg.maxSpeed <= 0) {
                cfg.maxSpeed = Double.NaN;
            }
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.minSpeed = Epics.get(channelName + ".VBAS", Double.class);
            if (cfg.minSpeed <= 0) {
                cfg.minSpeed = Double.NaN;
            }
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.precision = Epics.get(channelName + ".PREC", Integer.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.resolution = Epics.get(channelName + ".RDBD", Double.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        try {
            cfg.unit = Epics.get(channelName + ".EGU", String.class);
        } catch (ChannelException | java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
        cfg.save();
    }

    final DeviceListener changeListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                if ((device == state) || (device == done) || (device == enabled)) {
                    readStatus();   //To trigger state change event
                } else if (device == setpoint) {
                    setCache(value);
                }
            } catch (IOException | InterruptedException ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }
    };

    public void startHoming(boolean positive) throws IOException, InterruptedException {
        assertWriteEnabled();
        if (!isSimulated()) {
            try {
                Epics.putq(channelName + (positive ? ".HOMF" : ".HOMR"), 1);
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DeviceException(ex.getMessage());
            }
        }
    }

    public boolean isHoming() throws IOException, InterruptedException {
        if (isSimulated()) {
            return false;
        }
        try {
            return ((Epics.get(channelName + ".HOMR", Short.class) == 1)
                    || (Epics.get(channelName + ".HOMF", Short.class) == 1));
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex.getMessage());
        }
    }

    @Override
    public void startJog(boolean positive) throws IOException, InterruptedException {
        assertWriteEnabled();
        if (isSimulated()) {
            super.startJog(positive);
        } else {
            try {
                Epics.putq(channelName + (positive ? ".JOGF" : ".JOGR"), 1);
                if (readMode() != modeMove) {
                    writeMode(modeMove);
                }
            } catch (InterruptedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DeviceException(ex.getMessage());
            }
        }
    }

    public boolean isJogging() throws IOException, InterruptedException {
        if (isSimulated()) {
            return false;
        }
        try {
            return ((Epics.get(channelName + ".JOGF", Short.class) == 1)
                    || (Epics.get(channelName + ".JOGR", Short.class) == 1));
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DeviceException(ex.getMessage());
        }
    }

    @Override
    protected void doStartReferencing() throws IOException, InterruptedException {
        switch (getConfig().homingType) {
            case Backward:
                startHoming(false);
                break;
            case Forward:
                startHoming(true);
                break;
        }
    }

    public ChannelInteger getStateChannel() {
        return state;
    }

    public ChannelShort getDoneChannel() {
        return done;
    }

    static final Mode modeMove = Mode.Go;
    static final Mode modeStop = Mode.Stop;

    @Override
    protected void doStartMove(Double destination) throws IOException, InterruptedException {
        if (readMode() != modeMove) {
            writeMode(modeMove);
        }
        setpoint.write(destination);
        if (!isTrustedWrite()) {
            Double confirm = setpoint.read();
            if (Math.abs(confirm - destination) > Math.abs(getResolution())) {
                throw new DeviceException("Cannot write value to setpoint: " + destination);
            }
            setCache(confirm);
        }
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        stop.write(1);
        waitReady(10000);
        setpoint.writeAsync(getReadback().read());
    }

    @Override
    protected Double doReadDestination() throws IOException, InterruptedException {
        return setpoint.getValue(); //take if is monitored
    }

    @Override
    protected MotorStatus doReadStatus() throws IOException, InterruptedException {
        MotorStatus ret = new MotorStatus();

        int st = state.getValue();       //take if is monitored
        boolean doneMove = (done.getValue() != 0);       //take if is monitored
        boolean enabled = this.enabled == null ? true : this.enabled.getValue() == 0;

        boolean positiveDirection = (st & BitMask.BIT0) != 0;
        boolean stopped = doneMove && ((st & BitMask.BIT1) != 0);
        boolean hardLimitInf = (st & BitMask.BIT13) != 0;
        boolean hardLimitSup = (st & BitMask.BIT2) != 0;
        boolean homingSwitch = (st & BitMask.BIT3) != 0;
        boolean followingError = (st & BitMask.BIT6) != 0;
        boolean problem = (st & BitMask.BIT9) != 0;
        boolean com_error = (st & BitMask.BIT12) != 0;
        boolean homed = (st & BitMask.BIT14) != 0;

        /*        
         1. DIRECTION: last raw direction; (0:Negative, 1:Positive) 
         2. DONE: motion is complete. 
         3. PLUS_LS: plus limit switch has been hit. 
         4. HOMELS: state of the home limit switch. X03DA-ES2-MA:THT_able.VAL
         5. Unused 
         6. POSITION: closed-loop position control is enabled. 
         7. SLIP_STALL: Slip/Stall detected (eg. fatal following error) 
         8. HOME: if at home position. 
         9. PRESENT: encoder is present. 
         10. PROBLEM: driver stopped polling, or hardware problem 
         11. MOVING: non-zero velocity present. 
         12. GAIN_SUPPORT: motor supports closed-loop position control. 
         13. COMM_ERR: Controller communication error. 
         14. MINUS_LS: minus limit switch has been hit. 
         15. HOMED: the motor has been homed. 
         */
        //Only in Motor record R6-4
        if (version < 6.4) {
            homed = true;
        }

        ret.enabled = enabled;
        ret.stopped = stopped;
        ret.onHardLimitSup = hardLimitSup;
        ret.onHardLimitInf = hardLimitInf;
        ret.referencing = false;//Todo this flag can be set based on HOMR and HOMF
        ret.referenced = homed;
        ret.error = problem | com_error | followingError;
        return ret;
    }

    Mode readMode() throws IOException, InterruptedException {
        String str = mode.getValue(); //take if monitored
        try {
            return Mode.valueOf(str);
        } catch (Exception ex) {
            throw new DeviceException("Invalid mode: " + str);
        }
    }

    void writeMode(Mode mode) throws IOException, InterruptedException {
        this.mode.write(mode.toString());
    }

    public enum Mode {

        Go,
        Move,
        Pause,
        Stop
    }

    @Override
    public void setTrustedWrite(boolean value) {
        super.setTrustedWrite(value);
        setpoint.setTrustedWrite(value);
        setpoint.setBlockingWrite(!value);
    }

    class ReadbackChannel extends ChannelDouble {

        ReadbackChannel(String name, String channelName) {
            super(name, channelName, Motor.this.getPrecision());
            setParent(Motor.this);
            setAccessType(AccessType.Read);
        }

        @Override
        protected Double convertFromRead(Double value) {
            return Motor.this.convertFromRead(value);
        }
    }
}
