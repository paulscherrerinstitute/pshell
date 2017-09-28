package ch.psi.utils;

/**
 * Interface used by Chrono.waitCondition to check if condition is met.
 */
public interface Condition {

    boolean evaluate() throws InterruptedException;
}
