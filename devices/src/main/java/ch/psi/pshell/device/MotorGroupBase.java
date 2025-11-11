package ch.psi.pshell.device;

import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Reflection.Hidden;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of MotorGroup controlling a set of motors in simultaneous move. The simultaneity
 * is not reliable as it is done by software, and acceleration time and backlash are disregarded.
 * For precision moves, extensions could implement hardware based control.
 */
public class MotorGroupBase extends DeviceBase implements MotorGroup {

    boolean dynamicChangeDestination = true; //TODO: should configure it?
    volatile boolean executingSimultaneousMove = false;
    final ArrayList<Motor> simultaneousMoveStartingMotors = new  ArrayList<>();
    AtomicInteger simultaneousMoveCount = new AtomicInteger(0);

    public MotorGroupBase(String name) {
        super(name);
    }

    public MotorGroupBase(String name, Motor... motors) {
        super(name);
        setComponents(motors);
    }
    
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        simultaneousMoveCount.set(0);
    }

    @Override
    protected void setComponents(Device[] components) {
        for (Device dev : getComponents()) {
            dev.removeListener(changeListener);
        }
        super.setComponents(components);

        for (Device dev : components) {
            dev.addListener(changeListener);
        }
    }

    //These constructors are provided for device configuration edition panel
    public MotorGroupBase(String name, Motor m1) {
        this(name, new Motor[]{m1});
    }

    public MotorGroupBase(String name, Motor m1, Motor m2) {
        this(name, new Motor[]{m1, m2});
    }

    public MotorGroupBase(String name, Motor m1, Motor m2, Motor m3) {
        this(name, new Motor[]{m1, m2, m3});
    }

    public MotorGroupBase(String name, Motor m1, Motor m2, Motor m3, Motor m4) {
        this(name, new Motor[]{m1, m2, m3, m4});
    }

    public MotorGroupBase(String name, Motor m1, Motor m2, Motor m3, Motor m4, Motor m5) {
        this(name, new Motor[]{m1, m2, m3, m4, m5});
    }

    public MotorGroupBase(String name, Motor m1, Motor m2, Motor m3, Motor m4, Motor m5, Motor m6) {
        this(name, new Motor[]{m1, m2, m3, m4, m5, m6});
    }
    
    boolean restoreSpeedAfterMove = false;
            
    @Override
    public void setRestoreSpeedAfterMove(boolean value){
        restoreSpeedAfterMove = value;
    }
    
    @Override
    public boolean getRestoreSpeedAfterMove(){
        return restoreSpeedAfterMove;
    }
    
    @Override
    public void restoreSpeed() throws IOException, InterruptedException{
        for (Motor m : getMotors()) {
            m.restoreSpeed();
        }
    }

    @Override
    public void write(double[] value) throws IOException, InterruptedException {
        assertWriteEnabled();
        startMove(value, MoveMode.defaultSpeed, -1);
    }

    @Override
    public double[] read() throws IOException, InterruptedException {       
        Motor[] motors = getMotors();
        double[] ret = new double[motors.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = motors[i].read();
        }
        return ret;
    }

    @Override
    public void move(double[] destinations, MoveMode mode, double time) throws IOException, InterruptedException {
        assertWriteEnabled();
        startMove(destinations, mode, time);
        waitReady((int) ((mode == MoveMode.timed) ? (time * 1000) + 5000 : time));
        assertInPosition(destinations);
    }

    void startMove(double[] destinations, MoveMode mode, double time) throws IOException, InterruptedException {
        assertArgumentOk(destinations);
        updateState();
        Motor[] motors = getMotors();
        for (Motor m : motors) {
            m.readStatus(); //Update state
        }

        if (dynamicChangeDestination) {
            if ((getState() != State.Ready) && ((getState() != State.Busy))) {
                throw new DeviceBase.DeviceStateException();
            }
        } else {
            if (getState().isRunning()) {
                stop();
                assertState(State.Ready);
            }
        }
        executingSimultaneousMove = true;
        
        triggerValueChanging(destinations);

        for (int i = 0; i < motors.length; i++) {
            if ((motors[i].getState() != State.Ready) && (motors[i].getState() != State.Busy)) {
                throw new DeviceException("Invalid motor state");
            }
            if (motors[i].takeStatus().referencing) {
                throw new DeviceException("Ongoing referencing");
            }
            if (!Double.isNaN(destinations[i])) {
                motors[i].assertValidValue(destinations[i]);
                if (motors[i] instanceof DeviceBase deviceBase) {
                    deviceBase.triggerValueChanging(destinations[i]); //Anticipate exception if there is a restriction on the motor move
                }
            }
        }

        double[] move_speed = getMoveSpeeds(motors, destinations, mode, time);

        synchronized(simultaneousMoveStartingMotors){
            simultaneousMoveStartingMotors.clear();
        }
                            
        ArrayList<Motor> movingMotors = new ArrayList();
        for (int i = 0; i < motors.length; i++) {
            try {
                Motor m = motors[i];
                if (!Double.isNaN(move_speed[i])) {
                    movingMotors.add(m);
                    simultaneousMoveStartingMotors.add(m);
                    //Unchanged if < 0
                    if (move_speed[i] > 0) {
                        m.setSpeed(move_speed[i]);
                    }
                    simultaneousMoveCount.incrementAndGet();
                    m.moveAsync(destinations[i]).handle((ret,ex)->{
                        int count = simultaneousMoveCount.decrementAndGet();
                        if (count<=0) {
                            executingSimultaneousMove = false;
                            if (count<0){
                                simultaneousMoveCount.set(0);
                            }
                        }
                        if (restoreSpeedAfterMove){
                            try {
                                m.restoreSpeed();
                            } catch (Exception e) {
                                Logger.getLogger(MotorGroupBase.class.getName()).log(Level.WARNING, null, e);
                            }
                        }
                        return ret;
                    });
                }
            } catch (IOException | InterruptedException ex) {
                stop();
                throw ex;
            }
        }
        if (movingMotors.size() > 0) {
            setState(State.Busy);
        }
    }

    @Override
    public void moveRel(double[] offset, MoveMode mode, double time) throws IOException, InterruptedException {
        assertWriteEnabled();
        assertArgumentOk(offset);
        double[] positions = getPosition();
        double[] destinations = new double[positions.length];
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = positions[i] + offset[i];
        }
        move(destinations, mode, time);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        updateState();
        getPosition();
    }

    void updateState() {
        Motor[] motors = getMotors();
        synchronized(simultaneousMoveStartingMotors){
            for (Motor m: simultaneousMoveStartingMotors.toArray(new Motor[0])){
                if (m.getState() != State.Ready){      
                    simultaneousMoveStartingMotors.remove(m);
                }
            }    
        }
        for (Motor m : motors) {
            if (!m.getState().isNormal()) {
                setState(State.Invalid);
                return;
            }
        }
        for (Motor m : motors) {
            if (m.getState().isProcessing()) {
                setState(State.Busy);
                return;
            }
        }
        setState(State.Ready);
    }

    @Override
    public Motor[] getMotors() {
        ArrayList<Motor> ret = new ArrayList<>();
        for (Device dev : getComponents()) {
            if (dev instanceof Motor motor) {
                ret.add(motor);
            }
        }
        return ret.toArray(new Motor[0]);
    }

    protected void assertArgumentOk(double[] arg) throws InvalidValueException {
        if ((arg == null) || (arg.length != getMotors().length)) {
            throw new InvalidValueException(arg);
        }
    }

    public class MoveException extends IOException {

        MoveException(double[] position) {
            super("Not in position: " + Arrays.toString(position));
        }
    }

    @Hidden
    @Override
    public void assertInPosition(double[] position) throws IOException, InterruptedException {
        assertArgumentOk(position);
        if (!isInPosition(position)) {
            throw new MoveException(position);
        }

    }

    boolean isInPosition(Motor motor, double pos, double deadband) throws IOException, InterruptedException {
        return Math.abs(motor.getPosition() - pos) <= deadband;
    }

    @Override
    public boolean isInPosition(double[] position, double deadband) throws IOException, InterruptedException {
        if (Double.isNaN(deadband)) {
            return isInPosition(position);
        }
        assertArgumentOk(position);
        Motor[] motors = getMotors();
        for (int i = 0; i < motors.length; i++) {
            if ((!Double.isNaN(position[i])) && (!isInPosition(motors[i], position[i], deadband))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isInPosition(double[] position) throws IOException, InterruptedException {
        assertArgumentOk(position);
        Motor[] motors = getMotors();
        for (int i = 0; i < motors.length; i++) {
            if ((!Double.isNaN(position[i])) && (!motors[i].isInPosition(position[i]))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void waitInPosition(double[] position, int timeout) throws IOException, InterruptedException {
        assertArgumentOk(position);
        Chrono chrono = new Chrono();
        Motor[] motors = getMotors();
        for (int i = 0; i < motors.length; i++) {
            motors[i].waitInPosition(position[i], timeout);
            if (timeout > 0) {
                timeout = Math.max(0, timeout - chrono.getEllapsed());
            }
        }
    }

    @Override
    public double[] getPosition() throws IOException, InterruptedException {
        if (updatingCache){
            return (double[]) take();
        }         
        Motor[] motors = getMotors();
        double[] ret = new double[motors.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = motors[i].getPosition();
        }
        setCache(ret);
        return ret;
    }
    
    @Override
    public boolean isExecutingSimultaneousMove(){
        return executingSimultaneousMove;
    }    
    
    @Override
    public boolean isStartingSimultaneousMove(){
        if (executingSimultaneousMove){
            //If in startMoce or didn't start all motor moves
            if ((getState()==State.Ready) || (simultaneousMoveStartingMotors.size()>0)){
                return true;
            }
        }
        return false;
    }        
    
    public boolean getDynamicChangeDestination () {
        return dynamicChangeDestination;
    }
    
    public void setDynamicChangeDestination (boolean value) {
        dynamicChangeDestination = value;
    }    

    final DeviceListener changeListener = new ReadbackDeviceListener() {
        @Override
        public void onStateChanged(Device device, State state, State former) {
            updateState();
        }

        @Override
        public void onReadbackChanged(Device device, Object value) {
            Motor[] motors = getMotors();
            double[] pos = new double[motors.length];
            for (int i = 0; i < pos.length; i++) {
                Motor m = motors[i];
                if ((m == null) || (m.getReadback() == null)) {
                    pos[i] = Double.NaN;
                } else {
                    Double val = m.getReadback().take();
                    pos[i] = val == null ? Double.NaN : m.getReadback().take();
                }
            }
            setCache(pos);
        }
    };

    @Override
    public void stop() throws IOException, InterruptedException {
        assertWriteEnabled();
        for (final Motor m : getMotors()) {
            new Thread(() -> {
                try {
                    m.stop();
                } catch (Exception ex) {
                    getLogger().log(Level.INFO, null, ex);
                }
            }).start();
        }
    }

    //Async initial checks          
    @Override
    public CompletableFuture moveAsync(double[] destinations, MoveMode mode, double time) {
        assertArgumentOk(destinations);
        return MotorGroup.super.moveAsync(destinations, mode, time);
    }

    @Override
    public CompletableFuture moveRelAsync(double[] offset, int timeout) {
        assertArgumentOk(offset);
        return MotorGroup.super.moveRelAsync(offset, timeout);
    }

    //THis is just to fix the interface to Jython ()
    @Override
    public void move(double[] destination) throws IOException, InterruptedException {
        assertWriteEnabled();
        move(destination, TIMEOUT_INFINITE);
    }

    public double[] getMoveSpeeds(Motor[] motors, double[] destinations, MoveMode mode, double time) throws IOException, InterruptedException {
        double[] positions = getPosition();
        double[] modeSpeeds = new double[motors.length];
        double[] relativeSpeeds = new double[motors.length];
        double[] speeds = new double[motors.length];
        boolean[] isMoving = new boolean[motors.length];
        int movingCount = 0;
        double slowestSpeedFactor = (mode == MoveMode.timed) ? 1.0 : 0.0;

        for (int i = 0; i < motors.length; i++) {
            modeSpeeds[i] = Double.NaN;
            if (mode != MoveMode.timed) {
                if (mode == MoveMode.currentSpeed) {
                    modeSpeeds[i] = motors[i].getSpeed();
                } else if (mode == MoveMode.maximumSpeed) {
                    modeSpeeds[i] = motors[i].getMaxSpeed();
                }
                if (Double.isNaN(modeSpeeds[i])) {
                    modeSpeeds[i] = motors[i].getDefaultSpeed();
                }
            }

            double distance = Math.abs(destinations[i] - positions[i]);
            if (distance >= Math.abs(motors[i].getDeadband())) {
                isMoving[i] = true;
                movingCount++;
                if (mode == MoveMode.timed) {
                    relativeSpeeds[i] = distance / time;
                } else {
                    relativeSpeeds[i] = distance;
                    if ((slowestSpeedFactor == 0) || (relativeSpeeds[i] * slowestSpeedFactor > modeSpeeds[i])) {
                        slowestSpeedFactor = modeSpeeds[i] / relativeSpeeds[i];
                    }
                }
            } else {
                isMoving[i] = false;
            }
        }

        for (int i = 0; i < motors.length; i++) {
            if (isMoving[i]) {
                //We should not set the speed
                if (Double.isNaN(motors[i].getDefaultSpeed())) {
                    speeds[i] = -1;
                } else if ((movingCount == 1) && (mode != MoveMode.timed)) {  //If only one moves then overide calculations
                    speeds[i] = modeSpeeds[i];
                } else {
                    speeds[i] = relativeSpeeds[i] * slowestSpeedFactor;
                }
                speeds[i] = MotorBase.limitSpeed(motors[i], speeds[i]); //TODO: Should block movement or go as fast as possible?
            } else {
                speeds[i] = Double.NaN;
            }
        }
        return speeds;
    }

}
