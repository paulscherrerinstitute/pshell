package ch.psi.pshell.xscan;

import ch.psi.pshell.xscan.model.Configuration;
import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Manage the serialization and deserialization of the Xscan data model
 */
public class ModelManager {

    /**
     * De-serialize an instance of the Xscan data model
     *
     * @param file	File to deserialize
     * @throws JAXBException	Something went wrong while unmarshalling
     * @throws SAXException	Cannot read model schema file
     */
    public static Configuration unmarshall(File file) throws JAXBException, SAXException {

        JAXBContext context = JAXBContext.newInstance(Configuration.class);
        Unmarshaller u = context.createUnmarshaller();

        // Validation
        SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source s = new StreamSource(Configuration.class.getResourceAsStream("model-v1.xsd"));
        Schema schema = sf.newSchema(new Source[]{s}); // Use schema reference provided in XML
        u.setSchema(schema);

        try {
            Configuration model = (Configuration) u.unmarshal(new StreamSource(file), Configuration.class).getValue();
            return (model);
        } catch (UnmarshalException e) {
            // Check 
            if (e.getLinkedException() instanceof SAXParseException) {
                throw new RuntimeException("Configuration file does not comply to required model specification\nCause: " + e.getLinkedException().getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Serialize an instance of the Xscan data model
     *
     * @param model	Model datastructure
     * @param file	File to write the model data into
     * @throws JAXBException	Something went wrong while marshalling model
     * @throws SAXException	Cannot read model schema files
     */
    public static void marshall(Configuration model, File file) throws JAXBException, SAXException {
        QName qname = new QName("http://www.psi.ch/~ebner/models/scan/1.0", "configuration");

        JAXBContext context = JAXBContext.newInstance(Configuration.class);
        Marshaller m = context.createMarshaller();
        m.setProperty("jaxb.formatted.output", true);

        // Validation
        SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source s = new StreamSource(Configuration.class.getResourceAsStream("model-v1.xsd"));
        Schema schema = sf.newSchema(new Source[]{s}); // Use schema reference provided in XML
        m.setSchema(schema);

        m.marshal(new JAXBElement<Configuration>(qname, Configuration.class, model), file);
    }
}
