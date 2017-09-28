package ch.psi.pshell.device;

import ch.psi.pshell.core.Nameable;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented by positioners in scans.
 */
public interface Writable<T> extends Nameable {

    void write(T value) throws IOException, InterruptedException;

    default CompletableFuture writeAsync(T value) {
        return Threading.getFuture(() -> write(value));
    }

    public interface WritableNumber<T extends Number> extends Writable<T> {
    }

    public interface WritableArray<T> extends Writable<T> {

        public int getSize();
    }
}
