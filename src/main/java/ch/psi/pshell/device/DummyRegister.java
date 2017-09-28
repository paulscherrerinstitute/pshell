package ch.psi.pshell.device;

import java.io.IOException;

/**
 * A simulated register implementation.
 */
public class DummyRegister extends RegisterBase<Double> {

    Double value;

    /**
     * Persisted configuration
     */
    protected DummyRegister(String name, RegisterConfig config) {
        super(name, config);
    }

    /*
     * Volatile configuration
     */
    public DummyRegister() {
        this(null);
    }

    public DummyRegister(String name) {
        this(name, -1);
    }

    public DummyRegister(String name, int precision) {
        super(name, precision);
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
}
