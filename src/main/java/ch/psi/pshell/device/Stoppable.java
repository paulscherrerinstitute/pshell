package ch.psi.pshell.device;

import ch.psi.pshell.core.Nameable;
import ch.psi.utils.Threading;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface implemented by devices able to stop during a value change.
 */
public interface Stoppable extends Nameable{

    void stop() throws IOException, InterruptedException;

    default CompletableFuture stopAsync() {
        return Threading.getPrivateThreadFuture(() -> stop());
    }
}
