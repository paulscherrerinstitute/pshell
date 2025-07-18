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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 * <p>Java class for ArrayDetector complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayDetector">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.psi.ch/~ebner/models/scan/1.0}ComplexDetector">
 *       &lt;attribute name="arraySize" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayDetector")
public class ArrayDetector
    extends ComplexDetector
    implements Serializable, VariableSolver
{

    private final static long serialVersionUID = 1L;
    @XmlAttribute(name = "arraySize", required = true)
    protected int arraySize;
    @XmlAttribute(name = "arraySizeVar")
    protected String arraySizeVar;    

    /**
     * Gets the value of the arraySize property.
     * 
     */
    public int getArraySize() {
        return getInt(arraySizeVar,  arraySize);
    }

    /**
     * Sets the value of the arraySize property.
     * 
     */
    public void setArraySize(int value) {
        this.arraySize = value;
    }
    
     /**
     * Gets the value of the arraySizeVar property.
     * 
     */
    public String getArraySizeVar() {
        return arraySizeVar;
    }

    /**
     * Sets the value of the arraySizeVa property.
     * 
     */
    public void setArraySizeVar(String value) {
        this.arraySizeVar = value;
    }   

}
