package ch.psi.pshell.epics;

/**
 * Slit implementation constructed with the motor names only (EPICS motors internally created).
 */
public class Slit extends ch.psi.pshell.device.Slit {

    public Slit(String name, String bladePos, String bladeNeg) {
        super(name,
                new Motor(name + " pos blade", bladePos),
                new Motor(name + " neg blade", bladeNeg));
    }
}
