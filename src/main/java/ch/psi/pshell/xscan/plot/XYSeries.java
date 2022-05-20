package ch.psi.pshell.xscan.plot;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "xyseries")
public class XYSeries extends Series {

    private String x = null;
    private String y = null;
    private Integer maxItemCount = -1;

    public XYSeries() {
    }

    public XYSeries(String x, String y) {
        this.x = x;
        this.y = y;
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
    public Integer getMaxItemCount() {
        return maxItemCount;
    }

    public void setMaxItemCount(Integer maxCount) {
        this.maxItemCount = maxCount;
    }
}
