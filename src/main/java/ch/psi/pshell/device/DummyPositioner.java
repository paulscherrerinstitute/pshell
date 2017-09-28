package ch.psi.pshell.device;

import java.io.IOException;

/**
 * A simulated positioner implementation.
 */
public class DummyPositioner extends PositionerBase {

    Double value;

    public DummyPositioner(String name) {
        super(name, new PositionerConfig());
        try {
            initialize();
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        this.value = 0.0;
        setCache(value);
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        return value;
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        this.value = value;
    }

    @Override
    public Register<Double> getReadback() {
        return this;
    }

}
