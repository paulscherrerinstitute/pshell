package ch.psi.pshell.xscan.core;

/**
 * Guard to protect specific activities. A guard can be used to check for environment changes while a certain activity
 * was executed.
 *
 * Example: An Guard can be used to check whether an injection happened while the the detector were read.
 */
public interface Guard {

    /**
     * Initialize guard object and its internal state.
     */
    public void init();

    /**
     * Check the status of the guard.
     *
     * @return	Returns true if the guard condition was not constrainted since the last init call. False otherwise.
     */
    public boolean check();
}
