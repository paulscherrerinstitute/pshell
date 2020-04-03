package ch.psi.pshell.scripting;

import ch.psi.utils.Reflection.Hidden;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyTuple;

/**
 * Utiliy interfaces to make objects subscriptable in Python.
 */
public interface Subscriptable<T> {
    T __getitem__(int index);
    int __len__();
    
    default void _assertIndexValid(int index) {
            if ((index >= __len__()) || (index < 0)) {
                throw Py.IndexError("Invalid index: " + index + " (size: " + __len__() + ")");
            }        
    }
    
    public static interface Mapped<S, T> extends Subscriptable<T>{
        default T __getitem__(S key) {
            int index = toItemIndex(key);
            if (index < 0) {
                throw Py.IndexError("Invalid key: " + key);
            }
            return __getitem__(index);
        }
        default java.util.List<S> keys() {
            return getKeys();
        }

        default java.util.List<T> values() {
            java.util.List<T> ret = new java.util.ArrayList<>();
            for (S key : keys()){
                ret.add(__getitem__(key));
            }
            return ret;
        }

        default java.util.List<List> items() {
            java.util.List<List> ret = new java.util.ArrayList<>();
            for (S key : keys()){
               ret.add(Collections.unmodifiableList(Arrays.asList(key, __getitem__(key))));
            }
            return ret;
        }
        
        int toItemIndex(S itemKey);
        @Hidden
        java.util.List<S> getKeys();
    }

    public static interface SubscriptableList<T> extends Subscriptable<T>{

        @Override
        default T __getitem__(int index) {
            _assertIndexValid(index);
            return getValues().get(index);
        }

        @Override
        default int __len__() {
            java.util.List list = getValues();
            return (list == null) ? 0 : list.size();
        }

        java.util.List<T> getValues();
                
        //Backward compatibility
        @Hidden
        default java.util.List<T> getItemsList(){
            return getValues();
        }

    }

    public static interface MappedList<S,T> extends SubscriptableList<T>, Mapped<S,T> {        
        
    }

    public static interface SubscriptableArray<T> extends Subscriptable<T>{

        @Override
        default T __getitem__(int index) {
            _assertIndexValid(index);
            return getValues()[index];
        }

        @Override
        default int __len__() {
            T[] array = getValues();
            return (array == null) ? 0 : array.length;
        }

        T[] getValues();
        
        //Backward compatibility
        @Hidden
        default T[] getItemsArray(){
            return getValues();
        }        
    }

    public static interface MappedArray<S,T> extends SubscriptableArray<T> , Mapped<S,T>{

    }

    
    public static interface Sequence<T> extends Subscriptable<T>{

        @Override
        default T __getitem__(int index) {
            if ((index >= __len__()) || (index < 0)) {
                throw Py.IndexError("Invalid index: " + index + " (size: " + __len__() + ")");
            }
            return getItem(index);
        }

        @Override
        default int __len__() {
            return getLenght();
        }

        T getItem(int index);

        int getLenght();
    }    
    
    public static interface MappedSequence<S,T> extends Sequence<T> , Mapped<S,T>{

    }    
}
