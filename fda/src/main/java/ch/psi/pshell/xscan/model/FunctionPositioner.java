//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.27 at 11:18:50 AM CEST 
//


package ch.psi.pshell.xscan.model;

import ch.psi.pshell.xscan.VariableSolver;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for FunctionPositioner complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FunctionPositioner">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.psi.ch/~ebner/models/scan/1.0}DiscreteStepPositioner">
 *       &lt;sequence>
 *         &lt;element name="start" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="end" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="stepSize" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="function" type="{http://www.psi.ch/~ebner/models/scan/1.0}Function"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FunctionPositioner", propOrder = {
    "start",
    "end",
    "stepSize",
    "function",
    "startVar",
    "endVar",
    "stepSizeVar"            
})
public class FunctionPositioner
    extends DiscreteStepPositioner
    implements Serializable, VariableSolver
{

    private final static long serialVersionUID = 1L;
    protected double start;
    protected double end;
    protected double stepSize;
    @XmlElement(required = true)
    protected Function function;
    protected String startVar;
    protected String endVar;
    protected String stepSizeVar;    

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
     * Gets the value of the function property.
     * 
     * @return
     *     possible object is
     *     {@link Function }
     *     
     */
    public Function getFunction() {
        return function;
    }

    /**
     * Sets the value of the function property.
     * 
     * @param value
     *     allowed object is
     *     {@link Function }
     *     
     */
    public void setFunction(Function value) {
        this.function = value;
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
     * Sets the value of the stepSizeVar property.
     * 
     */
    public void setStepSizeVar(String value) {
        this.stepSizeVar = value;
    }    
}
