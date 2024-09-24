package ch.psi.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class MaxLenHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public MaxLenHashMap(int maxSize) {
        super(maxSize, 0.75f, true);  // true for access-order, false for insertion-order
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;  // Remove the eldest entry when size exceeds maxSize
    }    
    
    
    public static class OrderedMap<K extends Comparable<K>, V> extends TreeMap<K, V> {
        private final int maxSize;

        public OrderedMap(int maxSize) {
            super();  // Natural ordering of keys (Comparable)
            this.maxSize = maxSize;
        }

        @Override
        public V put(K key, V value) {
            V oldValue = super.put(key, value);  // Insert the new key-value pair
            if (size() > maxSize) {
                removeSmallestKey();  // Remove the smallest key if size exceeds the limit
            }
            return oldValue;
        }

        private void removeSmallestKey() {
            // TreeMap maintains keys in sorted order, so we can just remove the first (smallest) key
            K smallestKey = firstKey();
            remove(smallestKey);  // Remove the entry with the smallest key
        }
    }    
    
    public static void main(String[] args) {
        Map<Integer, String> map = new MaxLenHashMap<>(3);  // maxSize of 3
        map.put(2, "B");
        map.put(4, "D");
        map.put(1, "A");
        map.put(3, "C");
        System.out.println(map);  // Expected output: {4=D, 1=A, 3=C}
        
        map = new MaxLenHashMap.OrderedMap<Integer, String>(3);
        map.put(2, "B");
        map.put(4, "D");
        map.put(1, "A");
        System.out.println(map.keySet());
        map.put(3, "C");
        System.out.println(map);  // Expected output: {2=B, 3=C, 4=D}        
    }
}
