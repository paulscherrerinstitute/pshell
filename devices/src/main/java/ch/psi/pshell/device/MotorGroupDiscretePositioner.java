package ch.psi.pshell.device;

import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Config.ConfigListener;
import ch.psi.pshell.utils.State;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * DiscretePositioner having for each position name, a set of predefined motor positions to be
 * applied to a MotorGroup.
 */
public class MotorGroupDiscretePositioner extends DiscretePositionerBase {

    final MotorGroup motorGroup;
    final HashMap<String, double[]> motorPositions;

    /**
     * Entity class holding the configuration of an MotorGroupDiscretePositioner device.
     */
    public static class MotorGroupDiscretePositionerConfig extends RegisterConfig {

        public String[] positions;
        public String[] motor1;
        public String[] motor2;
        public String[] motor3;
        public String[] motor4;
        public String[] motor5;
        public String[] motor6;
        public String[] motor7;
        public String[] motor8;
    }

    @Override
    public MotorGroupDiscretePositionerConfig getConfig() {
        return (MotorGroupDiscretePositionerConfig) super.getConfig();
    }

    public MotorGroupDiscretePositioner(String name, MotorGroup motorGroup) {
        super(name, new MotorGroupDiscretePositionerConfig());
        this.motorGroup = motorGroup;
        motorGroup.addListener(changeListener);
        this.setComponents(new Device[]{motorGroup});
        motorPositions = new HashMap<>();
        getConfig().addListener(new ConfigListener() {
            @Override
            public void onSave(Config config) {
                try {
                    if (isInitialized()) {
                        parseConfig();
                        request();
                    }
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public MotorGroupDiscretePositioner(String name, Motor... motors) {
        this(name, new MotorGroupBase(name + " group", motors));
    }

    //These constructors are provided for device configuration edition panel
    public MotorGroupDiscretePositioner(String name, Motor m1) {
        this(name, new Motor[]{m1});
    }

    public MotorGroupDiscretePositioner(String name, Motor m1, Motor m2) {
        this(name, new Motor[]{m1, m2});
    }

    public MotorGroupDiscretePositioner(String name, Motor m1, Motor m2, Motor m3) {
        this(name, new Motor[]{m1, m2, m3});
    }

    public MotorGroupDiscretePositioner(String name, Motor m1, Motor m2, Motor m3, Motor m4) {
        this(name, new Motor[]{m1, m2, m3, m4});
    }

    public MotorGroupDiscretePositioner(String name, Motor m1, Motor m2, Motor m3, Motor m4, Motor m5) {
        this(name, new Motor[]{m1, m2, m3, m4, m5});
    }

    public MotorGroupDiscretePositioner(String name, Motor m1, Motor m2, Motor m3, Motor m4, Motor m5, Motor m6) {
        this(name, new Motor[]{m1, m2, m3, m4, m5, m6});
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        parseConfig();
        super.doInitialize();
    }

    void parseConfig() throws DeviceException {
        setPositions(getConfig().positions);
        motorPositions.clear();
        try {
            int numMotors = motorGroup.getMotors().length;
            for (int i = 0; i < getPositions().length; i++) {
                double[] pos = new double[numMotors];
                for (int j = 0; j < numMotors; j++) {
                    Field f = MotorGroupDiscretePositionerConfig.class.getField("motor" + (j + 1));
                    String[] motorPos = (String[]) f.get(getConfig());
                    pos[j] = Double.valueOf(motorPos[i]);
                }
                motorPositions.put(getPositions()[i], pos);
            }
        } catch (Exception ex) {
            throw new DeviceException(ex);
        }
    }

    @Override
    protected String doReadReadback() throws IOException, InterruptedException {
        for (String position : getPositions()) {
            double resolution = getConfig().precision < 0 ? Double.NaN : Math.pow(10.0, -getConfig().precision);
            if (motorGroup.isInPosition(motorPositions.get(position), resolution)) {
                return position;
            }
        }
        return UNKNOWN_POSITION;
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        motorGroup.stop();
    }

    @Override
    protected String doRead() throws IOException, InterruptedException {
        return take();
    }

    @Override
    protected void doWrite(String value) throws IOException, InterruptedException {
        if (motorPositions.containsKey(value)) {
            doMove(value, motorPositions.get(value));
        }
    }

    /**
     * Derived classes may implement other trajectories
     */
    protected void doMove(String positionName, double[] motorPositions) throws IOException, InterruptedException {
        motorGroup.write(motorPositions);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
    }

    final DeviceListener changeListener = new DeviceListener() {
        @Override
        public void onStateChanged(Device device, State state, State former) {
            try {
                if (state.isInitialized()) {
                    doUpdate();
                }
            } catch (Exception ex) {
                getLogger().log(Level.FINER, null, ex);
            }
        }

        @Override
        public void onValueChanged(Device device, Object value, Object former) {
            try {
                doUpdate();
            } catch (Exception ex) {
                getLogger().log(Level.FINER, null, ex);
            }
        }

    };

}
