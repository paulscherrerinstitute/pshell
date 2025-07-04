package ch.psi.pshell.utils;

import java.io.Serializable;
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
        for (Type type:Type.values()){
            if (type.getKey().equals(key)){
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid type: " + key);
    }
   
    public static Type fromString(String type){
        try{
            return Type.valueOf(type);
        } catch (Exception ex){            
            return fromKey(type);
        }
    }    
}
