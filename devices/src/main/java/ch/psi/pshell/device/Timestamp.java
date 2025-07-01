package ch.psi.pshell.device;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A register implementation returning a timestamp.
 */
public class Timestamp extends ReadonlyRegisterBase<Long> {

    Double value;
    long start;
    TimeUnit timeUnit;

    public Timestamp() {
        this(null);
    }

    public Timestamp(String name) {
        this(name, TimeUnit.MILLISECONDS);
        try {
            initialize();
        } catch (Exception ignore) {
        }
    }

    public Timestamp(String name, TimeUnit timeUnit) {
        super(name);
        this.timeUnit = timeUnit;
        try {
            initialize();
        } catch (Exception ignore) {
        }
    }

    public void reset() {
        start = System.nanoTime();
    }

    @Override
    protected Long doRead() throws IOException, InterruptedException {
        return timeUnit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }
}
