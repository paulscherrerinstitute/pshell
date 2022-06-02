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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ComplexDetector complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ComplexDetector">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.psi.ch/~ebner/models/scan/1.0}Detector">
 *       &lt;sequence>
 *         &lt;element name="preAction" type="{http://www.psi.ch/~ebner/models/scan/1.0}Action" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComplexDetector", propOrder = {
    "preAction"
})
@XmlSeeAlso({
    DetectorOfDetectors.class,
    ArrayDetector.class,
    ScalarDetector.class
})
public class ComplexDetector
    extends Detector
    implements Serializable, VariableSolver
{

    private final static long serialVersionUID = 1L;
    protected List<Action> preAction;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of the preAction property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the preAction property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPreAction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Action }
     * 
     * 
     */
    public List<Action> getPreAction() {
        if (preAction == null) {
            preAction = new ArrayList<Action>();
        }
        return this.preAction;
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
        return getString(name);
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

}
