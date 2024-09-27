package ch.psi.pshell.bs;

import ch.psi.pshell.device.Cacheable;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Readable.ReadableType;
import ch.psi.pshell.device.Startable;
import java.util.List;

/**
 *
 */
public interface StreamDevice extends  Readable<StreamValue>, Cacheable<StreamValue>, ReadableType, AddressableDevice, Startable{
    public List<Readable> getReadables();
    public void pause();
    public void resume();
    @Override
    public StreamValue take();
}
