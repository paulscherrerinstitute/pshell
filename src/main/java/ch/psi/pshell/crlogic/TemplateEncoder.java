package ch.psi.pshell.crlogic;

import ch.psi.jcae.Channel;
import ch.psi.jcae.annotation.CaChannel;

/**
 * Taken from FDA.
 */
public class TemplateEncoder {

    /**
     * Resolution	- $(P)$(E)_SCL
     */
    @CaChannel(type = Double.class, name = "${PREFIX}_SCL")
    private Channel<Double> resolution;

    /**
     * Offset	- $(P)$(E)_OFF
     */
    @CaChannel(type = Double.class, name = "${PREFIX}_OFF")
    private Channel<Double> offset;

    /**
     * Direction	- $(P)$(E)_DIR
     */
    public enum Direction {

        Negative, Positive
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}_DIR")
    private Channel<Integer> direction;

    public Channel<Double> getResolution() {
        return resolution;
    }

    public Channel<Double> getOffset() {
        return offset;
    }

    public Channel<Integer> getDirection() {
        return direction;
    }

}
