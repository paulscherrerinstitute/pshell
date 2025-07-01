package ch.psi.pshell.xscan.core;

public interface Action {

    /**
     * Execute logic of the action
     */
    public void execute() throws InterruptedException;

    /**
     * Abort the execution logic of the action
     */
    default public void abort() {
    }

    /**
     * Pause the execution logic of the action
     */
    default public void pause() {
    }

    /**
     * Resume the execution logic of the action
     */
    default public void resume() {
    }

    default public boolean isPaused() {
        return false;
    }
}
