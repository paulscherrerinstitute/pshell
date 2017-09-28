package ch.psi.pshell.device;

import java.io.IOException;

/**
 * Base class for readonly registers.
 */
public abstract class ReadonlyRegisterBase<T> extends RegisterBase<T> implements ReadonlyRegister<T> {

    /**
     * Persisted configuration
     */
    protected ReadonlyRegisterBase(String name, RegisterConfig config) {
        super(name, config);
    }

    /*
     * Volatile configuration
     */
    protected ReadonlyRegisterBase() {
        this(null);
    }

    protected ReadonlyRegisterBase(String name) {
        this(name, -1);
    }

    protected ReadonlyRegisterBase(String name, int precision) {
        super(name, precision);
        setAccessType(AccessType.Read);
    }

    @Override
    protected void doWrite(T value) throws IOException, InterruptedException {
    }

}
