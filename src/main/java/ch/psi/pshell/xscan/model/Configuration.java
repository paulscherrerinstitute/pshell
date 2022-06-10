//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.27 at 11:18:50 AM CEST 
//


package ch.psi.pshell.xscan.model;

import ch.psi.pshell.xscan.VariableSolver;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Configuration complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Configuration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="notification" type="{http://www.psi.ch/~ebner/models/scan/1.0}Notification" minOccurs="0"/>
 *         &lt;element name="data" type="{http://www.psi.ch/~ebner/models/scan/1.0}Data" minOccurs="0"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="variable" type="{http://www.psi.ch/~ebner/models/scan/1.0}Variable" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="scan" type="{http://www.psi.ch/~ebner/models/scan/1.0}Scan"/>
 *         &lt;element name="visualization" type="{http://www.psi.ch/~ebner/models/scan/1.0}Visualization" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="numberOfExecution" type="{http://www.w3.org/2001/XMLSchema}int" default="1" />
 *       &lt;attribute name="failOnSensorError" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Configuration", propOrder = {
    "notification",
    "data",
    "description",
    "variable",
    "scan",
    "visualization"
})
public class Configuration
    implements Serializable, VariableSolver
{

    private final static long serialVersionUID = 1L;
    protected Notification notification;
    protected Data data;
    protected String description;
    protected List<Variable> variable;
    @XmlElement(required = true)
    protected Scan scan;
    protected List<Visualization> visualization;
    @XmlAttribute(name = "numberOfExecution")
    protected Integer numberOfExecution;
    @XmlAttribute(name = "failOnSensorError")
    protected Boolean failOnSensorError;

    /**
     * Gets the value of the notification property.
     * 
     * @return
     *     possible object is
     *     {@link Notification }
     *     
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * Sets the value of the notification property.
     * 
     * @param value
     *     allowed object is
     *     {@link Notification }
     *     
     */
    public void setNotification(Notification value) {
        this.notification = value;
    }

    /**
     * Gets the value of the data property.
     * 
     * @return
     *     possible object is
     *     {@link Data }
     *     
     */
    public Data getData() {
        return data;
    }

    /**
     * Sets the value of the data property.
     * 
     * @param value
     *     allowed object is
     *     {@link Data }
     *     
     */
    public void setData(Data value) {
        this.data = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the variable property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the variable property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVariable().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Variable }
     * 
     * 
     */
    public List<Variable> getVariable() {
        if (variable == null) {
            variable = new ArrayList<Variable>();
        }
        return this.variable;
    }

    /**
     * Gets the value of the scan property.
     * 
     * @return
     *     possible object is
     *     {@link Scan }
     *     
     */
    public Scan getScan() {
        return scan;
    }

    /**
     * Sets the value of the scan property.
     * 
     * @param value
     *     allowed object is
     *     {@link Scan }
     *     
     */
    public void setScan(Scan value) {
        this.scan = value;
    }

    /**
     * Gets the value of the visualization property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the visualization property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVisualization().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Visualization }
     * 
     * 
     */
    public List<Visualization> getVisualization() {
        if (visualization == null) {
            visualization = new ArrayList<Visualization>();
        }
        return this.visualization;
    }

    /**
     * Gets the value of the numberOfExecution property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getNumberOfExecution() {
        if (numberOfExecution == null) {
            return  1;
        } else {
            return numberOfExecution;
        }
    }

    /**
     * Sets the value of the numberOfExecution property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberOfExecution(Integer value) {
        this.numberOfExecution = value;
    }

    /**
     * Gets the value of the failOnSensorError property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isFailOnSensorError() {
        if (failOnSensorError == null) {
            return true;
        } else {
            return failOnSensorError;
        }
    }

    /**
     * Sets the value of the failOnSensorError property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFailOnSensorError(Boolean value) {
        this.failOnSensorError = value;
    }

}