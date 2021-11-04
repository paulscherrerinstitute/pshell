package ch.psi.pshell.scripting;

import ch.psi.utils.Arr;
import ch.psi.utils.Reflection.Hidden;
import java.beans.Transient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySlice;
import org.python.core.PyTuple;
import org.python.core.PyNone;

/**
 * Utiliy interfaces to make objects subscriptable in Python.
 */
public interface Subscriptable {
    int __len__();

    default int assertIndexValid(int index) {
        if  (index < 0){
            index = __len__() + index;
        }
        if ((index >= __len__()) || (index < 0)) {
            throw Py.IndexError("Invalid index: " + index + " (size: " + __len__() + ")");
        }
        return index;
    }

    /**
     * Basic type for subscriptable lists.
     */
    interface Lst<T> extends Subscriptable {
        T __getitem__(int index);

        default List<T> __getitem__(PySlice slice) {
            int start =  (slice.getStart().getType() == PyNone.TYPE) ? 0: assertIndexValid(slice.getStart().asInt());
            int stop = (slice.getStop().getType() == PyNone.TYPE) ? __len__()-1 :assertIndexValid(slice.getStop().asInt());
            int step = (slice.getStep().getType() == PyNone.TYPE) ? 1 : slice.getStep().asInt();
            List<T> ret = new ArrayList<>();
            for (int i = start; i < stop; i += step) {
                ret.add(__getitem__(i));
            }
            return ret;
        }
    }

    /**
     * Basic type for subscriptable maps .
     */
    interface Dict<S, T> extends Subscriptable {
        T __getitem__(S key);

        default S assertIndexValid(S index) {
            if (!Arr.containsEqual(keys().toArray(),index)) {
                throw Py.IndexError("Invalid index: " + index);
            }
            return index;
        }

        List<S> keys();

        default List<T> values() {
            List<T> ret = new java.util.ArrayList<>();
            for (S key : keys()){
                ret.add(__getitem__(key));
            }
            return ret;
        }

        default List<List> items() {
            List<List> ret = new java.util.ArrayList<>();
            for (S key : keys()){
                ret.add(Collections.unmodifiableList(Arrays.asList(key, __getitem__(key))));
            }
            return ret;
        }

    }

    /**
     * Basic subscriptable list with index control.
     */
    interface Sequence<T> extends Lst<T>{
        @Override
        default T __getitem__(int index) {
            index = assertIndexValid(index);
            return getItem(index);
        }
        @Override
        default int __len__() {
            return getLenght();
        }

        T getItem(int index);

        int getLenght();
    }

    /**
     * Basic subscriptable map with index control.
     */
    interface Map<S, T> extends Dict<S,T> {
        default T __getitem__(S key) {
            key = assertIndexValid(key);
            return  getItem(key);
        }

        T getItem(S key);
    }

    /**
     * Subscriptable list backed by a Java List.
     */
    interface SubscriptableList<T> extends Lst<T>{

        @Override
        default T __getitem__(int index) {
            index = assertIndexValid(index);
            return getValues().get(index);
        }

        @Override
        default int __len__() {
            List list = getValues();
            return (list == null) ? 0 : list.size();
        }

        List<T> getValues();
                
        //Backward compatibility
        @Hidden
        @Transient
        default List<T> getItemsList(){
            return getValues();
        }

    }

    /**
     * Subscriptable list backed by a Java array.
     */
    interface SubscriptableArray<T> extends Lst<T>{

        @Override
        default T __getitem__(int index) {
            index = assertIndexValid(index);
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

    /**
     * A map that can be accesses also by order of element.
     */
    interface OrderedMap<S, T> extends Lst<T>, Dict<S, T>{
        default T __getitem__(S key) {
            int index = toItemIndex(key);
            if (index < 0) {
                throw Py.IndexError("Invalid key: " + key);
            }
            return __getitem__(index);
        }
        default List<S> keys() {
            return getKeys();
        }

        int toItemIndex(S itemKey);
        @Hidden
        List<S> getKeys();
    }

    /**
     * OrderedMap with index control.
     */
    interface MappedSequence<S,T> extends Sequence<T> , OrderedMap<S,T>{

    }

    /**
     * OrderedMap backed by a Java list.
     */
    interface MappedList<S,T> extends SubscriptableList<T>, OrderedMap<S,T> {
        
    }

    /**
     * OrderedMap backed by a Java array.
     */
    interface MappedArray<S,T> extends SubscriptableArray<T> , OrderedMap<S,T>{

    }
}
