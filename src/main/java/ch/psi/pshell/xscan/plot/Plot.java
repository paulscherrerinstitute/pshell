package ch.psi.pshell.xscan.plot;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

@XmlSeeAlso({LinePlot.class, MatrixPlot.class})
@XmlTransient
public abstract class Plot {

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
