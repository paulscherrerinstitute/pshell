package ch.psi.pshell.xscan.ui;

/**
 * Metadata of a component. The metadata consists of properties of a component like whether the component is optional,
 * its default value, ...
 *
 */
public class ComponentMetadata {

    private final boolean mandatory;
    private final String defaultValue;

    public ComponentMetadata(boolean mandatory) {
        this(mandatory, "");
    }

    public ComponentMetadata(boolean mandatory, String defaultValue) {
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

}
