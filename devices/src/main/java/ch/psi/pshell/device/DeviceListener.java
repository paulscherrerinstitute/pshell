package ch.psi.pshell.device;

import ch.psi.pshell.utils.State;

/**
 * The listener interface for receiving device events.
 */
public interface DeviceListener {

    /**
     * Sent every time the device state changes.
     */
    default void onStateChanged(Device device, State state, State former) {}

    /**
     * Sent every time the device cache is updated (also when value does not change).
     */
    default void onCacheChanged(Device device, Object value, Object former, long timestamp, boolean valueChange)  {}

    /**
     * Sent every time the cache is updated and the value is not equal to former. The equality
     * comparison is implemented by DeviceBase.hasChanged() method, which can be redefined in
     * implementations.
     */
    default void onValueChanged(Device device, Object value, Object former)  {}

    /**
     * Sent before a value change. If listener throws an exception then the change is aborted.
     */
    default void onValueChanging(Device device, Object value, Object former) throws Exception  {}

}
