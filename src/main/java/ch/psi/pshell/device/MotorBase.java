package ch.psi.pshell.device;

import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Base class for motor devices.
 */
public abstract class MotorBase extends PositionerBase implements Motor {
    
    final int CHRONO_MOVE_START_TIMEOUT = 1000;

    protected MotorBase(String name, MotorConfig config) {
        super(name, config);
        setVelocity(new RegisterBase<Double>(null, getPrecision()) {
                @Override
                protected void doWrite(Double value) throws IOException, InterruptedException {
                }

                @Override
                protected Double doRead() throws IOException, InterruptedException {
                    return doReadVelocity();
                }
            }
        );
        try {
            velocity.initialize();
        } catch (Exception ex) {
            //Can't happen
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (velocity == null) {
            throw new DeviceException("No velocity register defined");
        }
        currentStatus = null;

        super.doInitialize();
    }

    protected void setVelocity(Register<Double> velocity) {
        this.velocity = velocity;
        velocity.addListener(velocityChangeListener);
    }

    final DeviceListener velocityChangeListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                triggerSpeedChanged((Double) value);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    };

    protected void triggerSpeedChanged(Double value) {
        for (DeviceListener listener : getListeners()) {
            if (listener instanceof MotorListener) {
                try {
                    ((MotorListener) listener).onSpeedChanged(this, value);
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void triggerMotorStatusChanged(MotorStatus value) {
        for (DeviceListener listener : getListeners()) {
            if (listener instanceof MotorListener) {
                try {
                    ((MotorListener) listener).onMotorStatusChanged(this, value);
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (getVelocity() != null) {
            if (getVelocity().isMonitored() != value) {
                getVelocity().setMonitored(value);
            }
        }
    }

    Register<Double> velocity;

    @Override
    public Register<Double> getVelocity() {
        if (isSimulated()) {
            return simulatedVelocity;
        }
        return velocity;
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        assertInitialized();
        try {
            double readout = doReadDestination();
            getLogger().log(Level.FINER, "Setpoint readout= " + readout);
            return readout;
        } catch (IOException ex) {
            getLogger().log(Level.FINE, null, ex);
            setState(State.Offline);
            throw ex;
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        readStatus();
        getPosition();
        if (isSimulated()) {
            ((ReadonlyRegisterBase) getReadback()).setCache((Double) simulatedPosition);
        }
    }

    volatile Chrono chronoMoveStart;
    volatile Chrono chronoMoveStop;

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        if (isSimulated()) {

        } else {
            int retries = Math.max(getConfig().startRetries, 1);
            for (int retry = 1; retry <= retries; retry++) {
                try {
                    doStartMove(value);
                    //In some motors the stopped flag is not changing immediately
                    chronoMoveStart = new Chrono();
                    readStatus();
                    break;
                } catch (IOException ex) {
                    if (retry >= retries) {
                        throw ex;
                    }
                    Thread.sleep(1000);
                    getLogger().log(Level.INFO, "Error starting move: " + ex.getMessage() + " - retry #" + retry + "...");
                }
            }
        }
    }

    @Override
    public void reference() throws IOException, InterruptedException {
        assertWriteEnabled();
        if (isSimulated()) {
            move(0.0);
        } else {
            doStartReferencing();
        }
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        assertWriteEnabled();
        if (isSimulated()) {
            write(simulatedPosition);
            setState(State.Ready);
        } else {
            chronoMoveStart = null;
            doStop();
        }
    }

    @Override
    public void move(Double destination, int timeout) throws IOException, InterruptedException {
        assertWriteEnabled();
        try {
            super.move(destination, timeout);
        } finally {
            if (restoreSpeedAfterMove) {
                try {
                    restoreSpeed();
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }
    }

    boolean restoreSpeedAfterMove = false;

    @Override
    public void setRestoreSpeedAfterMove(boolean value) {
        restoreSpeedAfterMove = value;
    }

    @Override
    public boolean getRestoreSpeedAfterMove() {
        return restoreSpeedAfterMove;
    }

    @Override
    public void restoreSpeed() throws IOException, InterruptedException {
        setSpeed(getDefaultSpeed());
    }

    @Override
    public Double getPosition() throws IOException, InterruptedException {
        return getReadback().getValue(); //take if monitored
    }

    @Override
    public void setSpeed(Double speed) throws IOException, InterruptedException {
        assertWriteEnabled();
        if (!Double.isNaN(speed)) {
            if (speed <= 0) {
                throw new DeviceInvalidParameterException("Speed", speed);
            }
            speed = limitSpeed(speed);
            getVelocity().write(speed);
        }
    }

    protected double limitSpeed(double speed) {
        return limitSpeed(this, speed);
    }

    protected static double limitSpeed(Motor motor, double speed) {
        double max_speed = motor.getMaxSpeed();
        if (!Double.isNaN(max_speed)) {
            speed = Math.min(max_speed, speed);
        }
        double min_speed = motor.getMinSpeed();
        if (!Double.isNaN(min_speed)) {
            speed = Math.max(min_speed, speed);
        }
        return speed;
    }

    @Override
    public Double getSpeed() throws IOException, InterruptedException {
        return getVelocity().getValue(); //take if monitored
    }

    /**
     * Default moves to limit. Implementation may implement real jog.
     */
    @Override
    public void startJog(boolean positive) throws IOException, InterruptedException {
        assertWriteEnabled();
        Double destination;
        if (positive) {
            destination = getMaxValue();
            if (Double.isNaN(destination)) {
                destination = Double.POSITIVE_INFINITY;
            }
        } else {
            destination = getMinValue();
            if (Double.isNaN(destination)) {
                destination = Double.NEGATIVE_INFINITY;
            }
        }
        writeAsync(destination);
    }

    @Override
    public boolean isReady() throws IOException, InterruptedException {
        return readStatus().stopped;
    }

    MotorStatus currentStatus;

    @Override
    public MotorStatus takeStatus() {
        return currentStatus;
    }

    @Override
    public MotorConfig getConfig() {
        return (MotorConfig) super.getConfig();
    }

    @Override
    public MotorStatus readStatus() throws IOException, InterruptedException {
        assertInitialized();
        try {
            MotorStatus ret;
            if (isSimulated()) {
                ret = new MotorStatus();
                ret.onHardLimitSup = false;
                ret.onHardLimitInf = false;
                ret.referencing = false;
                ret.referenced = true;
                ret.error = false;
                ret.onSoftLimitSup = Math.abs(simulatedPosition - getConfig().maxValue) < Math.abs(getResolution());
                ret.onSoftLimitInf = Math.abs(simulatedPosition - getConfig().minValue) < Math.abs(getResolution());
                ret.stopped = Math.abs(simulatedPosition - take()) < Math.abs(getResolution()) / 2;
                ret.enabled = true;
            } else {
                ret = doReadStatus();
                if (getConfig().monitorByPosition) {
                    ret.stopped = isInPosition(take());
                }
                
                // Some motors need some time to clear the stopped flag after start of move
                if (chronoMoveStart != null) {
                    chronoMoveStop = null;
                    if (!ret.stopped || chronoMoveStart.isTimeout(CHRONO_MOVE_START_TIMEOUT) || isInPosition(take())) {
                        chronoMoveStart = null;//Now monitor based on flag only
                    } else {
                        ret.stopped = false; //Assumes beggining of move
                    }
                }
                
                //If estbilizationDelay is set, protects again oscilation in the end that
                //may make the stopped flag to bounce.
                if (getState() == State.Busy){
                    if (ret.stopped){                  
                        if (chronoMoveStop == null) {
                            if ((getConfig().estbilizationDelay >= 0)) {
                                chronoMoveStop = new Chrono();
                                ret.stopped = false;
                            }                        
                        } else {
                            if (chronoMoveStop.isTimeout(getConfig().estbilizationDelay)) {
                                chronoMoveStop = null;
                            } else {
                                ret.stopped = false;
                            }
                        } 
                    } else {
                        if (chronoMoveStop != null) {
                            chronoMoveStop = new Chrono();
                        }
                    }
                }
            }

            if (getState() != State.Fault) {
                if (!ret.enabled) {
                    setState(State.Disabled);
                } else if (ret.stopped) {
                    setState(State.Ready);
                } else {
                    setState(State.Busy);
                }
            }

            if (!ret.equals(currentStatus)) {
                triggerMotorStatusChanged(ret);
            }
            currentStatus = ret;
            getLogger().log(Level.FINEST, "Status = " + ret);
            return ret;
        } catch (IOException ex) {
            getLogger().log(Level.FINER, null, ex);
            if (ex instanceof DeviceTimeoutException) {
                setState(State.Offline);
            }
            throw ex;
        }
    }

    //Config
    @Override
    public Double getDefaultSpeed() {
        if (Double.isNaN(getConfig().defaultSpeed)) {
            return 1.0;
        }
        return getConfig().defaultSpeed;
    }

    @Override
    public Double getMinSpeed() {
        if (Double.isNaN(getConfig().minSpeed) || (getConfig().minSpeed <= 0.0)) {
            return getDefaultSpeed();
        }
        return getConfig().minSpeed;
    }

    @Override
    public Double getMaxSpeed() {
        if (Double.isNaN(getConfig().maxSpeed) || (getConfig().maxSpeed <= 0.0)) {
            return Double.NaN;
        }
        return getConfig().maxSpeed;
    }

    //State is processed when status is read
    @Override
    protected void processState() {
    }

    //Simulation        
    Register<Double> simulatedVelocity;
    volatile double simulatedPosition;

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        simulatedPosition = 0.0;

        if (simulatedVelocity == null) {
            simulatedVelocity = new DummyRegister(MotorBase.this.getName() + " simulated velocity", getPrecision());
            try {
                simulatedVelocity.initialize();
                simulatedVelocity.write(getDefaultSpeed());
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
            simulatedVelocity.addListener(velocityChangeListener);
        }
        startSimulationTimer();
    }

    @Override
    protected void onSimulationTimer() throws IOException, InterruptedException {
        if (((ReadonlyRegisterBase) getReadback()).take() == null) {
            ((ReadonlyRegisterBase) getReadback()).setCache((Double) simulatedPosition);
        }
        if (!isReady()) {
            double destination = take();
            double offset = take() - simulatedPosition;
            double speed = getSpeed() / 10.0;

            simulatedPosition = (offset < 0) ? simulatedPosition - speed : simulatedPosition + speed;
            if (Math.abs(destination - simulatedPosition) < speed) {
                simulatedPosition = destination;
            }
            simulatedPosition = Convert.roundDouble(simulatedPosition, getPrecision());
            if (isMonitored()) {
                ((ReadonlyRegisterBase) getReadback()).setCache((Double) simulatedPosition);
            }
        }
    }

    @Override
    protected Double getSimulatedValue() {
        return adjustPrecision(simulatedPosition);
    }

    //Overidables
    //These are not abstracts because they are not mandatory: setReadback and setVelocity can be used instead.
    protected Double doReadVelocity() throws IOException, InterruptedException {
        return null;
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        if (simulatedVelocity != null) {
            simulatedVelocity.removeListener(velocityChangeListener);
            simulatedVelocity = null;
        }
        if (velocity != null) {
            try {
                velocity.close();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, null, ex);
            }
        }
    }

    @Override
    /**
     * Set the current motor position to a given value changing the home offset.
     */
    public void setCurrentPosition(double value) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not supported.");
    }

    //Abstracts
    abstract protected Double doReadDestination() throws IOException, InterruptedException;

    abstract protected MotorStatus doReadStatus() throws IOException, InterruptedException;

    abstract protected void doStartReferencing() throws IOException, InterruptedException;

    abstract protected void doStartMove(Double destination) throws IOException, InterruptedException;

    abstract protected void doStop() throws IOException, InterruptedException;
}
