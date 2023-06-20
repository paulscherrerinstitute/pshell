package ch.psi.pshell.xscan.plot;

import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlSeeAlso({XYSeries.class, XYZSeries.class})
@XmlTransient
public abstract class Series {

}
