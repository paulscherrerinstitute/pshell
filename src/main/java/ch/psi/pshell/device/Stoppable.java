package ch.psi.pshell.device;

import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented by devices able to stop during a value change.
 */
public interface Stoppable {

    void stop() throws IOException, InterruptedException;

    default CompletableFuture stopAsync() {
        return Threading.getFuture(() -> stop());
    }
}
