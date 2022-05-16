package ch.psi.pshell.xscan.plot;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="vdescriptor")
public class VDescriptor {
	
	private List<Plot> plots = new ArrayList<>();

	@XmlElementWrapper
	@XmlElements({
        @XmlElement(name="lineplot",type=LinePlot.class),
        @XmlElement(name="matrixplot",type=MatrixPlot.class),
    })
	public List<Plot> getPlots() {
		return plots;
	}

	public void setPlots(List<Plot> plots) {
		this.plots = plots;
	}
}
