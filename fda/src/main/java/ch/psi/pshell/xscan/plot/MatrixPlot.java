package ch.psi.pshell.xscan.plot;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "matrixplot")
public class MatrixPlot extends Plot {

    private List<Series> data = new ArrayList<>();
    private Double minX;
    private Double maxX;
    private Integer nX;

    private Double minY;
    private Double maxY;
    private Integer nY;

    private String type;

    public MatrixPlot() {
    }

    public MatrixPlot(String title) {
        setTitle(title);
    }

    public List<Series> getData() {
        return data;
    }

    public void setData(List<Series> data) {
        this.data = data;
    }

    @XmlAttribute
    @XmlSchemaType(name = "float")
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
    public Integer getnX() {
        return nX;
    }

    public void setnX(Integer nX) {
        this.nX = nX;
    }

    @XmlAttribute
    public Double getMinY() {
        return minY;
    }

    public void setMinY(Double minY) {
        this.minY = minY;
    }

    @XmlAttribute
    public Double getMaxY() {
        return maxY;
    }

    public void setMaxY(Double maxY) {
        this.maxY = maxY;
    }

    @XmlAttribute
    public Integer getnY() {
        return nY;
    }

    public void setnY(Integer nY) {
        this.nY = nY;
    }

    @XmlAttribute
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
