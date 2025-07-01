package ch.psi.pshell.xscan.core;

/**
 * The sensor interface describes an entity that can be read out like a simple channel or (image) detector. Depending on
 * the sensor type the returned data is of a certain type.
 */
public interface Sensor {

    /**
     * Readout sensor.
     *
     * @return	Sensor value. The type of the returned value depends on the sensor type.
     */
    public Object read() throws InterruptedException;

    /**
     * Get the global id of the sensor
     *
     * @return	id of sensor
     */
    public String getId();
}
