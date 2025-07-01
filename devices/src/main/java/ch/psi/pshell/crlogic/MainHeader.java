package ch.psi.pshell.crlogic;

/**
 * Entity class, the header of ZMQ stream data received from the IOC.
 */
public class MainHeader {

    private String htype;
    private int elements;

    public void setElements(int elements) {
        this.elements = elements;
    }

    public int getElements() {
        return elements;
    }

    public void setHtype(String htype) {
        this.htype = htype;
    }

    public String getHtype() {
        return htype;
    }

}
