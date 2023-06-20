package ch.psi.pshell.xscan.plot;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * In a YZSeries the Y components hold an data array in which the index i the x and the value the y value.
 */
@XmlRootElement(name = "yzseries")
public class YZSeries extends Series {

    private String y = null;
    private String z = null;

    public YZSeries() {
    }

    public YZSeries(String y, String z) {
        this.y = y;
        this.z = z;
    }

    @XmlAttribute
    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    @XmlAttribute
    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }
}
