package ch.psi.pshell.device;

import ch.psi.pshell.device.Readable;
import java.io.IOException;

/**
 * A Readable capable of caching the read values. The cache is accessed with the
 * take() method.
 */
public interface Cacheable<T> extends Readable<T>, Timestamped {

    /**
     * Returns cache age (time since last update ) in milliseconds, or null if
     * no cache.
     */
    public Integer getAge();

    /**
     * Returns cache value (no device access)
     */
    public T take();

    /**
     * If cache available and cache more recent than maximumAge then returns
     * cache, otherwise reads from device.
     * If maximumAge is negative then returns cache if available, otherwise reads value.
     */
    default public T take(int maximumAge) throws IOException, InterruptedException {
        T value = take();
        if (value != null) {            
            if (maximumAge < 0){
                return value;
            }
            Integer age = getAge();
            if ((age != null) && (age <= maximumAge)) {
                return value;
            }
        }
        return read();
    }

    /**
     * Returns cache value and request update
     */
    public T request();

    public interface CacheableNumber<T extends Number> extends Cacheable<T> {

        @Override
        default public ReadableNumber<T> getCache() {
            return new CacheReadableNumber<T>() {
                @Override
                public T read() throws IOException, InterruptedException {
                    return take();
                }
                
                @Override
                public String getName() {
                    return CacheableNumber.this.getName();
                }
                
                @Override
                public String getAlias() {
                    return CacheableNumber.this.getAlias();
                }                

                @Override
                public Cacheable getParent() {
                    return CacheableNumber.this;
                }
            };
        }
    }

    public interface CacheableArray<T> extends Cacheable<T>, ReadableArray<T> {

        @Override
        default public ReadableArray<T> getCache() {
            return new CacheReadableArray<T>() {
                @Override
                public T read() throws IOException, InterruptedException {
                    return take();
                }

                @Override
                public int getSize() {
                    return CacheableArray.this.getSize();
                }

                @Override
                public String getName() {
                    return CacheableArray.this.getName();
                }

                @Override
                public String getAlias() {
                    return CacheableArray.this.getAlias();
                }     
                
                @Override
                public Cacheable getParent() {
                    return CacheableArray.this;
                }
            };
        }
    }

    public interface CacheableMatrix<T> extends Cacheable<T>, ReadableMatrix<T> {

        @Override
        default public ReadableMatrix<T> getCache() {
            return new CacheReadableMatrix<T>() {
                @Override
                public T read() throws IOException, InterruptedException {
                    return take();
                }

                @Override
                public int getWidth() {
                    return CacheableMatrix.this.getWidth();
                }

                @Override
                public int getHeight() {
                    return CacheableMatrix.this.getHeight();
                }

                @Override
                public String getName() {
                    return CacheableMatrix.this.getName();
                }

                @Override
                public String getAlias() {
                    return CacheableMatrix.this.getAlias();
                }     
                
                @Override
                public Cacheable getParent() {
                    return CacheableMatrix.this;
                }
            };
        }
    }
    
    public interface CacheableBoolean extends Cacheable<Boolean> {

        @Override
        default public ReadableBoolean getCache() {
            return new CacheReadableBoolean() {
                @Override
                public Boolean read() throws IOException, InterruptedException {
                    return take();
                }
                
                @Override
                public String getName() {
                    return CacheableBoolean.this.getName();
                }
                
                @Override
                public String getAlias() {
                    return CacheableBoolean.this.getAlias();
                }                     

                @Override
                public Cacheable getParent() {
                    return CacheableBoolean.this;
                }
            };
        }
    }

    public interface CacheableString extends Cacheable<String> {

        @Override
        default public ReadableString getCache() {
            return new CacheReadableString() {
                @Override
                public String read() throws IOException, InterruptedException {
                    return take();
                }
                
                @Override
                public String getName() {
                    return CacheableString.this.getName();
                }
                
                @Override
                public String getAlias() {
                    return CacheableString.this.getAlias();
                }                     

                @Override
                public Cacheable getParent() {
                    return CacheableString.this;
                }
            };
        }
    }
    
    /**
     * Returns a Readable on the cache value;
     */
    default public Readable<T> getCache() {
        return new CacheReadable<T>() {
            @Override
            public T read() throws IOException, InterruptedException {
                return take();
            }          

            @Override
            public String getName() {
                return Cacheable.this.getName();
            }
            
            @Override
            public String getAlias() {
                return Cacheable.this.getAlias();
            }                 

            //@Override
            public Cacheable getParent() {
                return Cacheable.this;
            }
        };
    }

    public interface CacheReadable<T> extends Readable<T>, Timestamped{

        public Cacheable getParent();
        
        @Override
        default public Long getTimestamp(){
            Cacheable parent = getParent();
            return (parent.getTimestamp());
        }
    }

    public interface CacheReadableNumber<T extends Number> extends CacheReadable<T>, ReadableNumber<T> {
    }

    public interface CacheReadableArray<T> extends CacheReadable<T>, ReadableArray<T> {
    }

    public interface CacheReadableMatrix<T> extends CacheReadable<T>, ReadableMatrix<T> {
    }
    
    public interface CacheReadableBoolean extends CacheReadable<Boolean>, ReadableBoolean {
    }

    public interface CacheReadableString extends CacheReadable<String>, ReadableString {
    }    
}
