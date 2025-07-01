package ch.psi.pshell.device;

/**
 * Move commands can be executed in a given time, or else using the current, default or maximum
 * motor(s) speed(s).
 */
public enum MoveMode {

    timed, currentSpeed, defaultSpeed, maximumSpeed

}
