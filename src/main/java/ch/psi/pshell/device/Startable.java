package ch.psi.pshell.device;
import java.io.IOException;

public interface Startable {
    public void start() throws IOException, InterruptedException;

    public void stop() throws IOException, InterruptedException;

    public boolean isStarted() throws IOException, InterruptedException;
}
