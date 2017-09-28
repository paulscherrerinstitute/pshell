package ch.psi.pshell.device;

import ch.psi.utils.Reflection.Hidden;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Base class for ControlledVariable implementations.
 */
public abstract class ControlledVariableBase extends ProcessVariableBase implements ControlledVariable {

    protected ControlledVariableBase(String name, ProcessVariableConfig cfg) {
        super(name, cfg);
        setReadback(new ReadonlyRegisterBase<Double>(name + " readback", getPrecision()) {
            @Override
            protected Double doRead() throws IOException, InterruptedException {
                return doReadReadback();
            }
        }
        );
        try {
            readback.initialize();
        } catch (Exception ex) {
            //Can't happen
        }
    }

    ReadonlyRegister<Double> readback;

    protected void setReadback(ReadonlyRegister<Double> readback) {
        this.readback = readback;
        if (readback != null) {
            readback.addListener(changeListener);
            onReadbackChanged(readback.take());
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
    }

    @Override
    public ReadonlyRegister<Double> getReadback() {
        if (isSimulated()) {
            return simulatedReadback;
        }
        return readback;
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        getReadback().read();
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        if (getReadback() != null) {
            if (getReadback().isMonitored() != value) {
                getReadback().setMonitored(value);
            }
        }
    }

    final DeviceListener changeListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                triggerReadbackChanged((Double) value);
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }
    };

    //Simulation    
    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        simulatedReadback = new ReadonlyRegisterBase(name + " readback", getPrecision()) {
            @Override
            protected Object doRead() throws IOException, InterruptedException {
                return ControlledVariableBase.this.getSimulatedValue();
            }
        };
        try {
            simulatedReadback.addListener(changeListener);
            simulatedReadback.initialize();
            simulatedReadback.setCache(0.0);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, null, ex);
        }
    }

    ReadonlyRegisterBase simulatedReadback;

    protected Double getSimulatedValue() {
        return take();
    }

    //Overidables
    //These are not abstracts because they are not mandatory: setReadback can be used instead.
    protected Double doReadReadback() throws IOException, InterruptedException {
        return null;
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        if (simulatedReadback != null) {
            simulatedReadback.close();
            simulatedReadback = null;
        }
    }

    public class PositionException extends DeviceException {

        public PositionException(Double destination) {
            super("Not in position: " + destination);
        }
    }

    @Override
    public Double getPosition() throws IOException, InterruptedException {
        return getReadback().getValue(); //take if monitored
    }

    @Override
    public boolean isInPosition(Double pos) throws IOException, InterruptedException {
        return Math.abs(getPosition() - pos) <= Math.abs(getResolution());
    }

    @Override
    public void waitInPosition(Double pos, int timeout) throws IOException, InterruptedException {
        try {
            getReadback().waitValueInRange(pos, getResolution(), timeout);
        } catch (IOException ex) {
            throw new PositionException(pos);
        }
    }

    @Hidden
    @Override
    public void assertInPosition(Double pos) throws IOException, InterruptedException {
        if (!isInPosition(pos)) {
            //Be sure to access device before asserting position
            if (getReadback().isMonitored()) {
                getReadback().update();
                if (isInPosition(pos)) {
                    return;
                }
            }
            throw new PositionException(pos);
        }
    }

}
