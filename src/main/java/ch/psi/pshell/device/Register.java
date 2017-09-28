package ch.psi.pshell.device;

/**
 * Interface for devices containing a raw numeric or array value.
 */
public interface Register<T> extends ReadonlyRegister<T>, Writable<T> {

    public void assertValidValue(T value) throws IllegalArgumentException;

    public interface RegisterNumber<T extends Number> extends Register<T>, ReadonlyRegisterNumber<T>, WritableNumber<T> {
    }

    public interface RegisterArray<T> extends Register<T>, ReadonlyRegisterArray<T>, WritableArray<T> {
    }

}
