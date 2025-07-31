package ch.psi.pshell.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
    public Set<Object> keySet() {
        return Collections.synchronizedSet(order);
    }
    
    @Override
    public synchronized Object put(Object key, Object value) {
        order.add(key);
        return super.put(key, value);
    }
    
    class Entry implements Map.Entry<Object,Object>{
        final Object key;
        Entry(Object key){
           this.key=key; 
        }
        @Override
        public Object getKey() {      
            return key;
        }

        @Override
        public Object getValue() {            
            return OrderedProperties.this.get(key);
        }

        @Override
        public Object setValue(Object value) {            
            return OrderedProperties.this.put(key, value);
        }
        
    }
    
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set ret = new LinkedHashSet<>();
        synchronized(this){
            for(Object key: keySet()){
                ret.add(new Entry(key));
            }
        }
        return Collections.synchronizedSet(ret);
    }

    @Override
    public Collection<Object> values() {
        List ret = new ArrayList();
        synchronized(this){
            for (Object key: order){
                ret.add(this.getOrDefault(key, null));
            }
        }
        return ret;
    }    
    
    @Override
    public Enumeration<Object> elements() {        
        return Collections.enumeration(values());
    }    
    
    @Override
    public synchronized void clear() {
        super.clear();
        order.clear();
    }    
    
    @Override
    public synchronized Object remove(Object key) {
        if (order.contains(key)){
            order.remove(key);
        }
        return super.remove(key);        
    }    
    
    @Override
    public boolean remove(Object key, Object value) {
        boolean ret = super.remove(key, value);
        if (ret){
            order.remove(key);
        }
        return ret;
    }
}
