package ch.psi.pshell.device;

import ch.psi.pshell.utils.Chrono;
import ch.psi.pshell.utils.Convert;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for Positioner implementations.
 */
public abstract class PositionerBase extends ControlledVariableBase implements Positioner {

    protected PositionerBase(String name, PositionerConfig cfg) {
        super(name, cfg);
    }

    @Override
    public PositionerConfig getConfig() {
        return (PositionerConfig) super.getConfig();
    }

    @Override
    protected void onValueChange(Object value, Object former) {
        processState();
        if (isSimulated() && isMonitored()) {
            getReadback().request();
        }
    }
    
    double adjustRotationPos(double pos){        
        if (getConfig().isRangeDefined()){
            if (!getConfig().isInRange(pos)){
                pos = Convert.toDegreesMin180To180(pos);
                if (!Double.isNaN(getConfig().minValue)){
                    if (pos < getConfig().minValue) {
                        pos += 360.0;
                    }
                } 
                if (!Double.isNaN(getConfig().maxValue)){
                    if (pos > getConfig().maxValue) {
                        pos -= 360.0;
                    } 
                }
            }
        }
        return pos;
    }
    
    @Override
    protected Double convertFromRead(Double value) {
        Double ret = super.convertFromRead(value);
        if (getConfig().rotation) {
            if (ret!=null){
                ret = adjustRotationPos(ret);
            }
        }
        return ret;
    }
       
    @Override
    public void write(Double value) throws IOException, InterruptedException {
        if (getConfig().rotation) {
            if (getConfig().isRangeDefined()){
                double current = read();
                double offset = Convert.toDegreesMin180To180(value - current);
                value = current + offset;
                if (value < getConfig().minValue) {
                    value += 360.0;
                    }
                if (value > getConfig().maxValue) {
                    value -= 360.0;
                }
            }
        }       
        super.write(value);
    }
    
    @Override
    public void move(Double destination, int timeout) throws IOException, InterruptedException {
        assertWriteEnabled();
        write(destination);
        waitReady(timeout);
        assertInPosition(destination);
    }

    @Override
    public void moveRel(Double offset, int timeout) throws IOException, InterruptedException {
        assertWriteEnabled();
        Double cache = take();
        double position = ((cache == null) ? read() : cache) + offset;
        move(position, timeout);
    }

    //Async initial checks      
    @Override
    public CompletableFuture moveAsync(Double value, int timeout) {
        assertValidValue(value);
        return Positioner.super.moveAsync(value, timeout);
    }

    @Override
    public CompletableFuture moveRelAsync(Double offset, int timeout) {
        //Do not make a read if cache absent: error will be thrown in executing thread
        Double cache = take();
        if (cache != null) {
            assertValidValue(cache + offset);
        }
        return Positioner.super.moveRelAsync(offset, timeout);
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        assertWriteEnabled();
        write(getPosition());
    }

    @Override
    public boolean isReady() throws IOException, InterruptedException {
        return isInPosition(take());
    }

    @Override
    protected void onReadbackChanged(Double value) {
        processState();
    }

    @Override
    public Double getPosition() throws IOException, InterruptedException {
        Double pos = super.getPosition();
        if (getConfig().rotation) {
            pos = adjustRotationPos(pos);
        }        
        return pos;
    }
    
    @Override
    public boolean isInPosition(Double pos) throws IOException, InterruptedException {
        Double offset = getPosition() - pos;
        if (getConfig().rotation) {
            offset = Convert.toDegreesOffset(offset);
        }
        return Math.abs(offset) <= Math.abs(getResolution());
    }

    @Override
    public void waitInPosition(Double pos, int timeout) throws IOException, InterruptedException {
        if (getConfig().rotation) {
            try {
                Chrono chrono = new Chrono();
                while (Math.abs(Convert.toDegreesOffset(getPosition() - pos)) > Math.abs(getResolution())) {
                    if ((timeout >= 0) && (chrono.isTimeout(timeout))) {
                        throw new DeviceTimeoutException("Timeout waiting value: " + pos);
                    }
                    Thread.sleep(getWaitSleep());
                }
            } catch (IOException ex) {
                throw new PositionException(pos);
            }
        } else {
            super.waitInPosition(pos, timeout);
        }
    }

    protected void processState() {
        if (getState().isNormal()) {
            Double setpoint = take();
            Double readout = getReadback().take();
            if ((readout != null) && (setpoint != null)) {
                Double offset = readout - setpoint;
                if (getConfig().rotation) {
                    offset = Convert.toDegreesOffset(offset);
                }
                if (Math.abs(offset) <= Math.abs(getResolution())) {
                    setState(State.Ready);
                } else {
                    setState(State.Busy);
                }
            }
        }
    }
}
