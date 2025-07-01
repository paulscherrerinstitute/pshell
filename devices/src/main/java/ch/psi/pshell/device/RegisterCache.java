package ch.psi.pshell.device;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Method read returns the cache of another register (help to implement scans based on cached
 * values).
 */
public class RegisterCache<T> extends ReadonlyRegisterBase<T> {

    final Cacheable<T> source;

    public RegisterCache(Cacheable<T> source) {
        this(source.getName(), (Cacheable) source);
    }

    public RegisterCache(String name, ReadonlyRegister<T> source) {
        this(name, (Cacheable) source);
    }

    public RegisterCache(String name, Cacheable<T> source) {
        super(name);
        boolean init = true;
        this.source = source;
        if (source instanceof DeviceBase deviceBase) {
            setParent(deviceBase);
            if (!deviceBase.isInitialized()) {
                init = false;
            }
        }
        if (init) {
            try {
                initialize();
            } catch (Exception ex) {
                Logger.getLogger(RegisterCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @Override
    protected T doRead() throws IOException, InterruptedException {
        return (T) source.take();
    }

    @Override
    public T take() {
        return (T) source.take();
    }

    @Override
    public Integer getAge() {
        return source.getAge();
    }

    @Override
    public Long getTimestamp() {
        return source.getTimestamp();
    }
}
