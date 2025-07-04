//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.07.27 at 11:18:50 AM CEST 
//


package ch.psi.pshell.xscan.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for MatrixPlotArray complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MatrixPlotArray">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.psi.ch/~ebner/models/scan/1.0}Visualization">
 *       &lt;attribute name="y" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="z" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="offset" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *       &lt;attribute name="size" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *       &lt;attribute name="type" default="2D">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="2D"/>
 *             &lt;enumeration value="3D"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MatrixPlotArray")
public class MatrixPlotArray
    extends Visualization
    implements Serializable
{

    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "y", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object y;
    @XmlAttribute(name = "z", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object z;
    @XmlAttribute(name = "offset")
    protected Integer offset;
    @XmlAttribute(name = "size")
    protected Integer size;
    @XmlAttribute(name = "type")
    protected String type;

    /**
     * Gets the value of the y property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setY(Object value) {
        this.y = value;
    }

    /**
     * Gets the value of the z property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getZ() {
        return z;
    }

    /**
     * Sets the value of the z property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setZ(Object value) {
        this.z = value;
    }

    /**
     * Gets the value of the offset property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getOffset() {
        if (offset == null) {
            return  0;
        } else {
            return offset;
        }
    }

    /**
     * Sets the value of the offset property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setOffset(Integer value) {
        this.offset = value;
    }

    /**
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getSize() {
        if (size == null) {
            return  0;
        } else {
            return size;
        }
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSize(Integer value) {
        this.size = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        if (type == null) {
            return "2D";
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
