package ch.psi.pshell.device;

import ch.psi.pshell.core.Nameable;

/**
 * Constants for readable and writable objects.
 */
public interface Record extends Nameable {
    public static final int SIZE_MAX = -1;
    public static final int SIZE_VALID = -2;
    public static final int KEEP_TO_VALID = -3;    
    
    public static final int UNDEFINED_PRECISION = -1;    
    public static final int RESOLVE_PRECISION = -2;
    
    public static final int TIMEOUT_INFINITE = -1;
    public static final int UNDEFINED = -1;    
}
