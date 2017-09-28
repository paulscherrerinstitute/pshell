package ch.psi.utils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * Property files where the entry order is respected.
 */
public class OrderedProperties extends Properties {

    private final HashSet order = new LinkedHashSet<>();

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(order);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        order.add(key);
        return super.put(key, value);
    }
}
