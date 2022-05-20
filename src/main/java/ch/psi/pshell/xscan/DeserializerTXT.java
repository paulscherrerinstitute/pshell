package ch.psi.pshell.xscan;

import ch.psi.utils.EventBus;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Deserializer for text files
 */
public class DeserializerTXT implements Deserializer {

    private static Logger logger = Logger.getLogger(DeserializerTXT.class.getName());

    private EventBus bus;
    private List<Metadata> metadata;
    private File file;

    private List<Integer> dindex;
    private List<Integer> iindex;

    public DeserializerTXT(EventBus b, File file) {
        this.bus = b;
        this.file = file;
        this.dindex = new ArrayList<Integer>();
        this.iindex = new ArrayList<Integer>();

        this.metadata = new ArrayList<>();
        try {
            // Read metadata
            // Open file
            BufferedReader reader = new BufferedReader(new FileReader(file));

            // Read file
            String line;

            // First line is id
            line = reader.readLine();
            line = line.replaceAll("^ *# *", "");
            String[] ids = line.split("\t");

            // Second line dimension
            line = reader.readLine();
            line = line.replaceAll("^ *# *", "");
            String[] dimensions = line.split("\t");

            // Create data message metadata
            Integer d = -1;
            for (int i = 0; i < ids.length; i++) {
                Integer dimension = Integer.parseInt(dimensions[i]);
                metadata.add(new Metadata(ids[i], dimension));

                // Store the first index of the first component
                // in each dimension ...
                if (!d.equals(dimension)) {
                    logger.finest("Add component index: " + i);
                    dindex.add(dimension);
                    iindex.add(i);
                    d = dimension;
                }
            }

            // Close file
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to read file metadata and initialize data queue", e);
        }
    }

    @Override
    public void read() {
        try {

            List<Double> checklist = new ArrayList<Double>(dindex.size());
            for (int i = 0; i < dindex.size(); i++) {
                checklist.add(null);
            }

            // Open file
            BufferedReader reader = new BufferedReader(new FileReader(file));

            // Read file
            String line;
            while ((line = reader.readLine()) != null) {

                // Ignore empty lines
                if (line.matches("^ *$")) {
                    continue;
                }

                // Ignore comment lines
                if (line.matches("^ *# *.*")) {
                    continue;
                }

                // Create and populate new data message
                String[] data = line.split("\t");
                DataMessage message = new DataMessage(metadata);
                for (String d : data) {
                    // Remove spaces at the end and beginning of the value
                    d = d.trim();

                    // If the String does not contains spaces we assume that it is a double
                    if (!d.contains(" ")) {
                        // Scalar value

                        Object value;
                        try {
                            value = new Double(d);
                        } catch (NumberFormatException e) {
                            // We treat it as a String
                            // TODO Need to find a way to treat other data formats
                            value = d;
                        }

                        // Add data to message
                        message.getData().add(value);
                    } else {
                        try {
                            // If the string contains spaces we treat it as an array
                            String[] values = d.split(" ");
                            double[] dv = new double[values.length];
                            for (int i = 0; i < values.length; i++) {
                                dv[i] = new Double(values[i]);
                            }
                            // Add data to message
                            message.getData().add(dv);
                        } catch (NumberFormatException e) {
                            // Workaround
                            // TODO Need to find a way to treat the array if it is not a double array
                            message.getData().add(new Object());
                        }
                    }

                }

                // Check whether to issue a end of dimension control message
                for (int t = 0; t < iindex.size(); t++) {
                    Integer i = iindex.get(t);
                    if (dindex.get(t) > 0) {
                        Double d = (Double) message.getData().get(i);
                        if (checklist.get(i) != null && !checklist.get(i).equals(d)) {
                            // If value changes issue a dimension delimiter message
                            bus.post(new StreamDelimiterMessage(dindex.get(t) - 1));
                        }
                        checklist.set(i, d);
                    }
                }

                // Put message to queue
                bus.post(message);

                // TODO Need to detect dimension boundaries
            }

            // Add delimiter for all the dimensions
            for (int i = dindex.size() - 1; i >= 0; i--) {
                bus.post(new StreamDelimiterMessage(dindex.get(i)));
            }

            // Place end of stream message
            bus.post(new EndOfStreamMessage());

            // Close file
            reader.close();

        } catch (IOException e) {
            throw new RuntimeException("Data deserializer had a problem reading the specified datafile", e);
        }

    }

}
