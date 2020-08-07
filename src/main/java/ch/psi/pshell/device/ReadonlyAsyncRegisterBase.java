package ch.psi.pshell.device;

import java.io.IOException;

/**
 * Base class for asynchronous registers. Implementations update cache asynchronously, by calling
 * onReadout.
 */
public abstract class ReadonlyAsyncRegisterBase<T> extends ReadonlyRegisterBase<T> {

    /**
     * Persisted configuration
     */
    protected ReadonlyAsyncRegisterBase(String name, RegisterConfig config) {
        super(name, config);
        setMonitored(true);
        setAccessType(AccessType.Read);
    }

    /*
     * Volatile configuration
     */
    protected ReadonlyAsyncRegisterBase() {
        this(null);
    }

    protected ReadonlyAsyncRegisterBase(String name) {
        this(name, -1);
    }

    protected ReadonlyAsyncRegisterBase(String name, int precision) {
        super(name, precision);
        setAccessType(AccessType.Read);
    }

    @Override
    protected T doRead() throws IOException, InterruptedException {
        return take();
    }

}
