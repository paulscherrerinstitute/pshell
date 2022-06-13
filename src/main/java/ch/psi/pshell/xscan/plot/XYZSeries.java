package ch.psi.pshell.xscan.plot;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "xyzseries")
public class XYZSeries extends Series {

    private String x = null;
    private String y = null;
    private String z = null;

    public XYZSeries() {
    }

    public XYZSeries(String x, String y, String z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @XmlAttribute
    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
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
