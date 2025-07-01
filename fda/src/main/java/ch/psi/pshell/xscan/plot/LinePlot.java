package ch.psi.pshell.xscan.plot;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "lineplot")
public class LinePlot extends Plot {

    private List<Series> data = new ArrayList<>();

    private Double minX;
    private Double maxX;
    private Integer maxSeries;

    public LinePlot() {
    }

    public LinePlot(String title) {
        setTitle(title);
    }

    @XmlElement
    public List<Series> getData() {
        return data;
    }

    public void setData(List<Series> data) {
        this.data = data;
    }

    @XmlAttribute
    public Double getMinX() {
        return minX;
    }

    public void setMinX(Double minX) {
        this.minX = minX;
    }

    @XmlAttribute
    public Double getMaxX() {
        return maxX;
    }

    public void setMaxX(Double maxX) {
        this.maxX = maxX;
    }

    @XmlAttribute
    public Integer getMaxSeries() {
        return maxSeries;
    }

    public void setMaxSeries(Integer maxSeries) {
        this.maxSeries = maxSeries;
    }
}
