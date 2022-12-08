package ch.psi.pshell.device;

import ch.psi.utils.Arr;
import ch.psi.utils.Reflection.Hidden;
import ch.psi.utils.State;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Base class for DiscretePositioner implementations.
 */
public abstract class DiscretePositionerBase extends RegisterBase<String> implements DiscretePositioner {

    String[] positions;

    public DiscretePositionerBase(String name, RegisterConfig config) {
        super(name, config); 
        setDefaultReadback();
    }

    public DiscretePositionerBase(String name, String... positions) {
        super(name);
        setPositions(positions);
        setDefaultReadback();
    }

    private void setDefaultReadback() {
        setReadback(new ReadonlyRegisterBase<String>(null, getPrecision()) {
            @Override
            protected String doRead() throws IOException, InterruptedException {
                return doReadReadback();
            }

            @Hidden
            @Override
            public Number takeAsNumber() {
                int index = getIndex(take());
                return (index >= 0) ? index : null;
            }
        }
        );
        try {
            readback.initialize();
        } catch (Exception ex) {
            //Can't happen
        }
    }

    protected void setPositions(String... positions) {
        this.positions = positions;
        if (positions != null) {
            for (int i = 0; i < positions.length; i++) {
                positions[i] = positions[i].trim();
            }
        }
    }

    @Override
    public String[] getPositions() {
        return positions;
    }

    ReadonlyRegister<String> readback;

    /**
     * Derived classes can call this to provide an existing register or implementing
     * getReadbackValue callback
     */
    public void setReadback(ReadonlyRegister<String> readback) {
        if (this.readback != null) {
            readback.removeListener(changeListener);
        }
        this.readback = readback;
        readback.addListener(changeListener);
        onReadbackChanged(readback.take());
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        if ((positions != null) && (positions.length > 0)) {
            setCache(positions[0]);
        }

    }

    @Override
    public ReadonlyRegister<String> getReadback() {
        if (isSimulated()) {
            return this;
        }
        return readback;
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        if (!isSimulated() || (positions == null)) {
            setCache(UNKNOWN_POSITION);
        }
        if (positions == null) {
            setState(State.Invalid);
        }
    }

    @Override
    public String getPosition() throws IOException, InterruptedException {
        return getReadback().getValue(); //take if monitored
    }

    @Override
    public void move(String destination, int timeout) throws IOException, InterruptedException {
        assertWriteEnabled();
        assertState(State.Ready);
        write(destination);
        update();
        waitReady(timeout);
        assertInPosition(destination);
    }

    @Override
    public boolean isValidValue(String value) {
        return Arr.containsEqual(getPositions(), value);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        try {
            getReadback().read(); //getReadback().update() generate blockings
            updateState();
        } catch (IOException ex) {
            setState(State.Fault);
            throw ex;
        }
    }

    protected String getSetpointValue() {
        return take();
    }

    protected void updateState() {
        String rback = getReadback().take();
        String setpt = getSetpointValue();
        boolean readbackDefined = (rback != null) && (!rback.equals(UNKNOWN_POSITION));
        boolean setpointDefined = (setpt != null) && (!setpt.equals(UNKNOWN_POSITION));
        if (!setpointDefined) {
            setState(State.Ready);
        } else {
            if ((!readbackDefined) || (!rback.equals(setpt))) {
                setState(State.Busy);
            } else {
                setState(State.Ready);
            }
        }
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

    public class MoveException extends DeviceException {

        public MoveException(String position) {
            super("Not in position: " + position);
        }
    }

    @Hidden
    @Override
    public void assertInPosition(String position) throws IOException, InterruptedException {
        if (!isInPosition(position)) {
            throw new MoveException(position);
        }

    }

    @Override
    public boolean isInPosition(String position) throws IOException, InterruptedException {
        return getPosition().equals(position);
    }

    @Override
    public void waitInPosition(String position, int timeout) throws IOException, InterruptedException {
        getReadback().waitValue(position, timeout);
    }

    final DeviceListener changeListener = new DeviceAdapter() {
        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                updateState();
                triggerReadbackChanged((String) value);
            } catch (Exception ex) {
                getLogger().log(Level.FINE, null, ex);
            }
        }
    };

    protected void triggerReadbackChanged(String value) {
        onReadbackChanged(value);
        for (DeviceListener listener : getListeners()) {
            if (listener instanceof ReadbackDeviceListener) {
                try {
                    ((ReadbackDeviceListener) listener).onReadbackChanged(this, value);
                } catch (Exception ex) {
                    getLogger().log(Level.WARNING, null, ex);
                }
            }
        }
    }

    protected void onReadbackChanged(String value) {
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        assertWriteEnabled();
        State state = getState();
        doStop();
        //if (state.isActive()){
        if (state == State.Busy) {
            if (!isSimulated()) {
                setCache(UNKNOWN_POSITION);
            }
            setState(State.Ready);
        }
    }

    //Overidables
    //These is not abstract because they are not mandatory: setReadback can be used instead.
    protected String doReadReadback() throws IOException, InterruptedException {
        return null;
    }

    //Abstracts
    protected void doStop() throws IOException, InterruptedException{
        
    }

}
