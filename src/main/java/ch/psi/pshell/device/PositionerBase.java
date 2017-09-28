package ch.psi.pshell.device;

import ch.psi.utils.Chrono;
import ch.psi.utils.Convert;
import ch.psi.utils.State;
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

    @Override
    public void move(Double destination, int timeout) throws IOException, InterruptedException {
        assertWriteEnabled();
        if (getConfig().rotation) {
            double current = read();
            double offset = Convert.toDegreesMin180To180(destination - current);
            destination = current + offset;
            if (destination < getConfig().minValue) {
                destination += 360.0;
            }
            if (destination > getConfig().maxValue) {
                destination -= 360.0;
            }
        }
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
                    Thread.sleep(10);
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
