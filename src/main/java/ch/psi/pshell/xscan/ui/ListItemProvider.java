package ch.psi.pshell.xscan.ui;

import java.awt.Component;
import java.util.List;

/**
 * Interface defining the functionality of a ListElement provider
 */
public interface ListItemProvider<T> {

    /**
     * Get list of keys of possible components this provider can create
     *
     * @return
     */
    public String[] getItemKeys();

    /**
     * Get a new list item instance for the given key
     *
     * @param key Key of the component to be created
     * @return New component for the given key. Returns null if there is no matching component for the key.
     */
    public Component newItem(String key);

    /**
     * Remove an item from the list
     *
     * @param component
     */
    public void removeItem(Component component);

    /**
     * Get a new list item that is initialized with the given object
     *
     * @param object
     * @return New GUI component for the passed object
     */
//    public Component getItem(T object);
    /**
     * Get currently managed items
     *
     * @return List of items
     */
    public List<Component> getItems();

    public boolean isEmpty();

    public int size();

    public void moveItemUp(Component component);

    public void moveItemDown(Component component);

}
