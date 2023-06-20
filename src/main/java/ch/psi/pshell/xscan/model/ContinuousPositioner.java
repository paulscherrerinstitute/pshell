//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.27 at 11:18:50 AM CEST 
//


package ch.psi.pshell.xscan.model;

import ch.psi.pshell.xscan.VariableSolver;
import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ContinuousPositioner complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ContinuousPositioner">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.psi.ch/~ebner/models/scan/1.0}Positioner">
 *       &lt;sequence>
 *         &lt;element name="start" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="stepSize" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="integrationTime" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="additionalBacklash" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="readback" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContinuousPositioner", propOrder = {
    "start",
    "end",
    "stepSize",
    "integrationTime",
    "additionalBacklash",
    "startVar",
    "endVar",
    "stepSizeVar",
    "integrationTimeVar",
    "additionalBacklashVar",    
})
public class ContinuousPositioner
    extends Positioner
    implements Serializable, VariableSolver
{

    private final static long serialVersionUID = 1L;
    protected double start;
    protected double end;
    protected double stepSize;
    protected double integrationTime;
    protected Double additionalBacklash;
    protected String startVar;
    protected String endVar;
    protected String stepSizeVar;
    protected String integrationTimeVar;
    protected String additionalBacklashVar;
    
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "readback")
    protected String readback;

    /**
     * Gets the value of the start property.
     * 
     */
    public double getStart() {
        return getDouble(startVar, start);
    }

    /**
     * Sets the value of the start property.
     * 
     */
    public void setStart(double value) {
        this.start = value;
    }

    /**
     * Gets the value of the end property.
     * 
     */
    public double getEnd() {
        return getDouble(endVar, end);
    }

    /**
     * Sets the value of the end property.
     * 
     */
    public void setEnd(double value) {
        this.end = value;
    }

    /**
     * Gets the value of the stepSize property.
     * 
     */
    public double getStepSize() {
        return getDouble(stepSizeVar, stepSize);
    }

    /**
     * Sets the value of the stepSize property.
     * 
     */
    public void setStepSize(double value) {
        this.stepSize = value;
    }

    /**
     * Gets the value of the integrationTime property.
     * 
     */
    public double getIntegrationTime() {
        return getDouble(integrationTimeVar, integrationTime);
    }

    /**
     * Sets the value of the integrationTime property.
     * 
     */
    public void setIntegrationTime(double value) {
        this.integrationTime = value;
    }

    /**
     * Gets the value of the additionalBacklash property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getAdditionalBacklash() {
        if ((additionalBacklashVar==null) && (additionalBacklash==null)){
            return null;
        }
        return getDouble(additionalBacklashVar, (additionalBacklash == null) ? 0.0D : additionalBacklash);
    }

    /**
     * Sets the value of the additionalBacklash property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAdditionalBacklash(Double value) {
        this.additionalBacklash = value;
    }
    
    /**
     * Gets the value of the startVar property.
     * 
     */
    public String getStartVar() {
        return startVar;
    }

    /**
     * Sets the value of the startVar property.
     * 
     */
    public void setStartVar(String value) {
        this.startVar = value;
    }

    /**
     * Gets the value of the endVar property.
     * 
     */
    public String getEndVar() {
        return endVar;
    }

    /**
     * Sets the value of the endVar property.
     * 
     */
    public void setEndVar(String value) {
        this.endVar = value;
    }

    /**
     * Gets the value of the stepSizeVar property.
     * 
     */
    public String getStepSizeVar() {
        return stepSizeVar;
    }

    /**
     * Sets the value of the integrationTimeVar property.
     * 
     */
    public void setStepSizeVar(String value) {
        this.stepSizeVar = value;
    }    
    
    

    /**
     * Gets the value of the stepSizeVar property.
     * 
     */
    public String getIntegrationTimeVar() {
        return integrationTimeVar;
    }

    /**
     * Sets the value of the integrationTimeVar property.
     * 
     */
    public void setIntegrationTimeVar(String value) {
        this.integrationTimeVar = value;
    }   


    /**
     * Gets the value of the additionalBacklashVar property.
     * 
     */
    public String getAdditionalBacklashVar() {
        return additionalBacklashVar;
    }

    /**
     * Sets the value of the additionalBacklashVar property.
     * 
     */
    public void setAdditionalBacklashVar(String value) {
        this.additionalBacklashVar = value;
    }       

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the readback property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReadback() {
        return readback;
    }

    /**
     * Sets the value of the readback property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReadback(String value) {
        this.readback = value;
    }

}
