package ch.psi.pshell.data;

import ch.psi.pshell.scripting.Subscriptable;
import ch.psi.utils.Convert;
import ch.psi.utils.IO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class loads data from a text file into a double[][] array.
 */
public class Table implements Subscriptable.MappedSequence<String, double[]>{

    String[] header;
    double[][] data;

    public Table(String[] header, double[][] data) {
        this.header = header;
        this.data = data;
    }

    public boolean isDefined() {
        return data != null;
    }

    public void assertDefined() throws IOException {
        if (!isDefined()) {
            throw new IOException("Undefined table");
        }
    }

    public String[] getHeader() {
        if (header == null) {
            return new String[0];
        }
        return header;
    }

    public double[][] getData() {
        return data;
    }

    public int getRows() {
        if (data == null) {
            return 0;
        }
        return data.length;
    }

    public int getCols() {
        if (header == null) {
            return 0;
        }
        return header.length;
    }

    public double[] getRow(int index) {
        if ((index < 0) || (index >= getRows())) {
            return null;
        }
        return data[index];
    }

    public double[] getCol(int index) {
        if ((index < 0) || (index >= getCols())) {
            return null;
        }
        int rows = getRows();
        double[] ret = new double[getRows()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (index >= data[i].length) ? Double.NaN : data[i][index];
        }
        return ret;
    }

    public int getColIndex(String name) {
        String[] header = getHeader();
        for (int i = 0; i < header.length; i++) {
            if (header[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public double[] getCol(String name) {
        return getCol(getColIndex(name));
    }

    public void save(String fileName, String separator) throws IOException {
        assertDefined();
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        ArrayList<String> lines = new ArrayList<>();
        lines.add(String.join(separator, getHeader()));

        for (int i = 0; i < getRows(); i++) {
            lines.add(String.join(separator, Convert.toStringArray(getRow(i))));
        }
        Files.write(Paths.get(fileName), lines);
    }

    public static Table load(String fileName) throws IOException {
        return load(fileName, "\\s+", "#");
    }

    public static Table load(String fileName, String separator, String comment) throws IOException {
        String[][] lines = IO.parse(fileName, separator, comment);

        if ((lines.length == 0) || (lines[0].length == 0)) {
            return new Table(null, null);
        }
        String[] header = lines[0];
        double[][] data = new double[lines.length - 1][header.length];

        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < header.length; col++) {
                try {
                    data[row][col] = Double.valueOf(lines[row + 1][col]);
                } catch (Exception ex) {
                    data[row][col] = Double.NaN;
                }
            }
        }
        return new Table(header, data);
    }
    
    /**
     * File with no table header
     */
   
    public static Table loadRaw(String fileName, String[] header) throws IOException {
        return loadRaw(fileName, "\\s+", "#", header);
    }

    public static Table loadRaw(String fileName, String separator, String comment, String[] header) throws IOException {
        String[][] lines = IO.parse(fileName, separator, comment);

        if ((lines.length == 0) || (lines[0].length == 0)) {
            return new Table(null, null);
        }
        
        double[][] data = new double[lines.length][header.length];
        
        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < header.length; col++) {
                try {
                    data[row][col] = Double.valueOf(lines[row][col]);
                } catch (Exception ex) {
                    data[row][col] = Double.NaN;
                }
            }
        }
        return new Table(header, data);
    }

    @Override
    public String toString() {
        return Table.class.getSimpleName() + ": columns=" + getCols() + ", rows=" + getRows();
    }

    public String print() {
        return print("\t");
    }

    public String print(String separator) {
        String lineSeparator = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(separator, header)).append(lineSeparator);
        if (data != null) {
            for (double[] row : data) {
                sb.append(String.join(separator, Convert.toStringArray(row))).append(lineSeparator);
            }
        }
        return sb.toString();
    }

    @Override
    public double[] getItem(int index) {
        return getCol(index);
    }

    @Override
    public int getLenght() {
        return getCols();
    }

    @Override
    public int toItemIndex(String itemKey) {
        return getColIndex(itemKey);
    }
    
    @Override
    public java.util.List<String> getKeys(){
        return Arrays.asList(getHeader());
       
    }

}

