package ch.psi.pshell.bs;

import ch.psi.pshell.device.RegisterConfig;

/**
 * Configuration for Scalar objects.
 */
public class ScalarConfig extends RegisterConfig {

    public String id;
    public int modulo = Scalar.DEFAULT_MODULO;
    public int offset = Scalar.DEFAULT_OFFSET;
}
