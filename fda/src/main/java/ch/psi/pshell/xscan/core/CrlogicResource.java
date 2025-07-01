package ch.psi.pshell.xscan.core;

/**
 * Readout resource of crlogic
 */
public class CrlogicResource {

    /**
     * Id of the data
     */
    private String id;
    /**
     * Id of the crlogic resource to be read out. As configured in the CRLOGIC part of the IOC startup script
     */
    private String key;
    /**
     * Flag whether to read back the delta of the values of this resource. (delta is done in software)
     */
    private boolean delta = false;

    public CrlogicResource() {
    }

    public CrlogicResource(String id, String key) {
        this.id = id;
        this.key = key;
    }

    public CrlogicResource(String id, String key, boolean delta) {
        this.id = id;
        this.key = key;
        this.delta = delta;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isDelta() {
        return delta;
    }

    public void setDelta(boolean delta) {
        this.delta = delta;
    }
}
