package ch.psi.pshell.utils;

/**
 * Enumeration representing state of objects.
 */
public enum State {

    Invalid,
    Initializing,
    Ready,
    Paused,
    Busy,
    Disabled,
    Closing,
    Fault,
    Offline;

    public class StateException extends Exception {

        StateException() {
            super("Invalid state: " + State.this.toString());
        }
    }

    public boolean isInitialized() {
        return (this != Invalid) && (this != Initializing) && (this != Closing);
    }

    public boolean isReady() {
        return (this == Ready);
    }

    public boolean isActive() {
        return isReady() || isProcessing();
    }

    public boolean isRunning() {
        return (this == Busy) || (this == Initializing);
    }

    public boolean isProcessing() {
        return isRunning() || (this == Paused);
    }

    public boolean isFault() {
        return (this == Fault) || (this == Offline);
    }

    public boolean isNormal() {
        return isInitialized() && !(isFault() && (this != Disabled));
    }

    public void assertInitialized() throws StateException {
        if (!isInitialized()) {
            throw new StateException();
        }
    }

    public void assertActive() throws StateException {
        if (!isActive()) {
            throw new StateException();
        }
    }

    public void assertRunning() throws StateException {
        if (!isRunning()) {
            throw new StateException();
        }
    }

    public void assertProcessing() throws StateException {
        if (!isProcessing()) {
            throw new StateException();
        }
    }

    public void assertNormal() throws StateException {
        if (!isNormal()) {
            throw new StateException();
        }
    }

    public void assertReady() throws StateException {
        if (this != Ready) {
            throw new StateException();
        }
    }

    public void assertIs(State state) throws StateException {
        if (this != state) {
            throw new StateException();
        }
    }

    public void assertNot(State state) throws StateException {
        if (this == state) {
            throw new StateException();
        }
    }
}
