package ch.psi.pshell.utils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigInteger;

public enum Type implements Serializable {       
   
   Bool("bool", Byte.BYTES),
   Int8("int8", Byte.BYTES),
   UInt8("uint8", Byte.BYTES, true),
   Int16("int16", Short.BYTES),
   UInt16("uint16", Short.BYTES, true),
   Int32("int32", Integer.BYTES),
   UInt32("uint32", Integer.BYTES, true),
   Int64("int64", Long.BYTES),
   UInt64("uint64", Long.BYTES, true),
   Float32("float32", Float.BYTES),
   Float64("float64", Double.BYTES),
   String("string", Integer.MAX_VALUE);

   public static final String ARRAY_SUFFIX = "[]";
   private final String key;
   private final int bytes;
   private final boolean unsigned;

   Type(String key, int bytes) {
      this(key, bytes, false);
   }

   Type(String key, int bytes, boolean unsigned) {
      this.key = key;
      this.bytes = bytes;
      this.unsigned = unsigned;
   }

   public String getKey() {
      return key;
   }

   public int getBytes() {
      return bytes;
   }
   
   public boolean isUnsigned() {
      return unsigned;
   }   
   
   public Class toClass(){
       return switch (this){
           case Bool -> Boolean.class;       
           case Int8 -> Byte.class;       
           case UInt8 -> Short.class;       
           case Int16 -> Short.class;       
           case UInt16 -> Integer.class;       
           case Int32 -> Integer.class;       
           case UInt32 -> Long.class;       
           case Int64 -> Long.class;       
           case UInt64 -> BigInteger.class;       
           case Float32 -> Float.class;       
           case Float64 -> Double.class;       
           case String -> String.class;                                         
       };   
   }
   
   public Class toPrimitiveArrayClass(){
       return switch (this){
           case Bool -> boolean[].class;       
           case Int8 -> byte[].class;     
           case UInt8 -> short[].class;     
           case Int16 -> short[].class;     
           case UInt16 -> int[].class;        
           case Int32 -> int[].class;       
           case UInt32 -> long[].class;          
           case Int64 -> long[].class;     
           case UInt64 -> BigInteger[].class;      
           case Float32 -> float[].class;      
           case Float64 -> double[].class;        
           case String -> String[].class;                                          
       };   
   }   

   public static Class toClass(String key){
        boolean array = key.endsWith(ARRAY_SUFFIX);
        if(array){
            key = key.substring(0, key.lastIndexOf(ARRAY_SUFFIX));
            return Type.fromKey(key).toPrimitiveArrayClass();
        }
        return Type.fromKey(key).toClass();
       
   }
   
   
   public static String toKey( Class type, boolean unsigned){        
        boolean array = type.isArray();
        if (array){
            type = Arr.getClassComponentType(type);
        }
        String ret = fromClass(type, unsigned).getKey();
        if (array){
            ret += ARRAY_SUFFIX;
        }
        return ret;
   }
   
   
   public static Type fromClass(Class cls){
       return fromClass(cls, false);
   }
   
   public static Type fromClass(Class cls, boolean unsigned){
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }        
        if (cls.isPrimitive()){
            cls = Convert.getWrapperClass(cls);
        }
        if (cls == Double.class){
            return  Type.Float64;
        }
        if (cls == Float.class){
            return  Type.Float32;
        }        
        if (cls == Byte.class){
            return Type.Int8;
        }
        if (cls == Short.class){
            return unsigned ? Type.UInt8 : Type.Int16;
        }
        if (cls == Integer.class){
            return unsigned ? Type.UInt16 : Type.Int32;
        }
        if (cls == Long.class){
            return unsigned ? Type.UInt32 : Type.Int64;
        }
        if (cls == BigInteger.class){
            return unsigned ? Type.UInt64 : Type.Int64;
        }
        if (cls == String.class){
            return Type.String;
        }
        if (cls == Boolean.class){
            return Type.Bool;
        }
        throw new IllegalArgumentException("Invalid class: " + cls);
    }
   
    public static Type fromKey(String key){
        boolean array = key.endsWith(ARRAY_SUFFIX);
        if(array){
            key = toComponentTypeKey(key);
        }        
        for (Type type:Type.values()){
            if (type.getKey().equals(key)){
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type: " + key);
    }
    
    public static Type componentTypeFromKey(String key){
       key = toComponentTypeKey(key);
       return fromKey(key);
    }
   
    public static String toComponentTypeKey(String key){
        if(key.endsWith(ARRAY_SUFFIX)){
            key = key.substring(0, key.lastIndexOf(ARRAY_SUFFIX));
        }        
        return key;
    }

    public static Type fromString(String type){
        try{
            return Type.valueOf(type);
        } catch (Exception ex){            
            return fromKey(type);
        }
    }    
}
