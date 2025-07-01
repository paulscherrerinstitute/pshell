package ch.psi.pshell.device;

import java.io.IOException;

/**
 * A simulated motor implementation.
 */
public class DummyMotor extends MotorBase {

    public DummyMotor(String name) {
        super(name, new MotorConfig());
        setSimulated();
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        if (getConfig().isUndefined()) {
            //If units not set assumes it is first execution 
            MotorConfig cfg = getConfig();
            cfg.minValue = -10.0;
            cfg.maxValue = 10.0;
            cfg.defaultSpeed = 1.0;
            cfg.maxSpeed = 10.0;
            cfg.minSpeed = 0.1;
            cfg.precision = 2;
            cfg.unit = "mm";
            cfg.save();
        }        
        super.doInitialize();
    }

    @Override
    protected void doStartReferencing() throws IOException, InterruptedException {
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
    }

    @Override
    protected MotorStatus doReadStatus() throws IOException, InterruptedException {
        return null;
    }

    @Override
    protected Double doReadDestination() throws IOException, InterruptedException {
        return take();
    }

    @Override
    protected void doStartMove(Double destination) throws IOException, InterruptedException {
    }
}
