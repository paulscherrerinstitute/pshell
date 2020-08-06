package ch.psi.pshell.device;

import ch.psi.utils.Reflection.Hidden;

/**
 * Interface for devices containing a raw numeric or array value.
 */
public interface Register<T> extends ReadonlyRegister<T>, Writable<T> {

    public void assertValidValue(T value) throws IllegalArgumentException;

    public interface RegisterNumber<T extends Number> extends Register<T>, ReadonlyRegisterNumber<T>, WritableNumber<T> {
    }

    public interface RegisterArray<T> extends Register<T>, ReadonlyRegisterArray<T>, WritableArray<T> {
        @Override
        default public int getSize() {            
            return ReadonlyRegisterArray.super.getSize();
        }           
    }

    public interface RegisterBoolean extends Register<Boolean>, ReadonlyRegisterBoolean, WritableBoolean {
    }

    public interface RegisterString extends Register<String>, ReadonlyRegisterString, WritableString {
    }    
}    
