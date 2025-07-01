package ch.psi.pshell.xscan.plot;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "vdescriptor")
public class VDescriptor {

    private List<Plot> plots = new ArrayList<>();

    @XmlElementWrapper
    @XmlElements({
        @XmlElement(name = "lineplot", type = LinePlot.class),
        @XmlElement(name = "matrixplot", type = MatrixPlot.class),})
    public List<Plot> getPlots() {
        return plots;
    }

    public void setPlots(List<Plot> plots) {
        this.plots = plots;
    }
}
