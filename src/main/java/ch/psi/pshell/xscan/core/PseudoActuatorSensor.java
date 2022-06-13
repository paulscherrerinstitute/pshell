package ch.psi.pshell.xscan.core;

/**
 * Pseudo actor that is literally doing nothing for n times
 */
public class PseudoActuatorSensor implements Actor, Sensor {

    /**
     * Execution count of actuator.
     */
    private int count;

    /**
     * Number of counts for this actuator
     */
    private final int counts;

    private final String id;

    /**
     * @param counts
     * @param id	Id of the Actor/Sensor
     */
    public PseudoActuatorSensor(String id, int counts) {
        if (counts < 1) {
            throw new IllegalArgumentException("Count [" + counts + "] must be > 0");
        }
        this.id = id;
        this.counts = counts;

        init();
    }

    @Override
    public void set() {
        if (!hasNext()) {
            throw new IllegalStateException("The actuator does not have any next step.");
        }

        count++;
    }

    @Override
    public boolean hasNext() {
        return (count < counts);
    }

    @Override
    public void init() {
        count = 0;
    }

    @Override
    public void reverse() {
    }

    @Override
    public void reset() {
    }

    @Override
    public Object read() {
        return new Double(count); // Return actual count
    }

    @Override
    public String getId() {
        return this.id;
    }
}
