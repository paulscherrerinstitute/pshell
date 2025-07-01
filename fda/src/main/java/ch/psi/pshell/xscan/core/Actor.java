package ch.psi.pshell.xscan.core;

public interface Actor {

    /**
     * Set actor value
     */
    public void set() throws InterruptedException;

    /**
     * Function to check whether the actor has a next set value
     *
     * @return Returns true if there is an actor value for the next iteration. False if there is no actor value (i.e.
     * this will be the last iteration in the ActionLoop)
     */
    public boolean hasNext();

    /**
     * Initialize the actor to the start
     */
    public void init();

    /**
     * Reverse the set values of the actor
     */
    public void reverse();

    /**
     * Reset the actuator to its initial configuration
     */
    public void reset();
}
