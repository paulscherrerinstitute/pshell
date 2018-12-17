package ch.psi.pshell.scripting;

import org.python.core.Py;

/**
 * Utiliy interfaces to make objects subscriptable in Python.
 */
public interface Subscriptable {
    Object __getitem__(int index);
    int __len__();
    
    default void _assertIndexValid(int index) {
            if ((index >= __len__()) || (index < 0)) {
                throw Py.IndexError("Invalid index: " + index + " (size: " + __len__() + ")");
            }        
    }
    
    public static interface Mapped<T> extends Subscriptable{
        default Object __getitem__(String key) {
            int index = toItemIndex(key);
            if (index < 0) {
                throw Py.IndexError("Invalid key: " + key);
            }
            return __getitem__(index);
        }

        int toItemIndex(String itemKey);    
    }

    public static interface SubscriptableList extends Subscriptable{

        @Override
        default Object __getitem__(int index) {
            _assertIndexValid(index);
            return getItemsList().get(index);
        }

        @Override
        default int __len__() {
            java.util.List list = getItemsList();
            return (list == null) ? 0 : list.size();
        }

        java.util.List getItemsList();
    }

    public static interface MappedList extends SubscriptableList, Mapped {
    }

    public static interface SubscriptableArray<T> extends Subscriptable{

        @Override
        default Object __getitem__(int index) {
            _assertIndexValid(index);
            return getItemsArray()[index];
        }

        @Override
        default int __len__() {
            T[] array = getItemsArray();
            return (array == null) ? 0 : array.length;
        }

        T[] getItemsArray();
    }

    public static interface MappedArray<T> extends SubscriptableArray<T> , Mapped{

    }

    
    public static interface Sequence<T> extends Subscriptable{

        @Override
        default Object __getitem__(int index) {
            if ((index >= __len__()) || (index < 0)) {
                throw Py.IndexError("Invalid index: " + index + " (size: " + __len__() + ")");
            }
            return getItemValue(index);
        }

        @Override
        default int __len__() {
            return getItemsLenght();
        }

        T getItemValue(int index);

        int getItemsLenght();
    }    
    
    public static interface MappedSequence<T> extends Sequence<T> , Mapped{

    }    
}
