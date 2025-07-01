package ch.psi.pshell.device;

import ch.psi.pshell.device.Register.RegisterString;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Reflection.Hidden;

/**
 * Movable device which position is defined by a string in a predefined set. The position is
 * "Unknown" if the device is not in a predefined position.
 */
public interface DiscretePositioner extends RegisterString, ReadbackDevice<String>, Movable<String> {

    final static String UNKNOWN_POSITION = "Unknown";

    public String[] getPositions();

    @Hidden
    public default int getIndex(String position) {
        return Arr.getIndexEqual(getPositions(), position);
    }

    @Hidden
    @Override
    default Number takeAsNumber() {
        int index = getIndex(take());
        return (index >= 0) ? index : null;
    }
}
