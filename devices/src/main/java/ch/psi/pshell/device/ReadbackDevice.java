package ch.psi.pshell.device;

/**
 * Interface for devices having a readback value.
 */
public interface ReadbackDevice<T> {

    public ReadonlyRegister<T> getReadback();
}
