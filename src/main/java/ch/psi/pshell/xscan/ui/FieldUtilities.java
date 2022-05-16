package ch.psi.pshell.xscan.ui;

import java.text.DecimalFormat;
import java.text.Format;


/**
 * Utility class for text fields
 */
public class FieldUtilities {

    /**
     * Get a properly formated DecimalFormat object
     * @return
     */
    public static Format getDecimalFormat(){
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(20);
        format.setGroupingUsed(false);
        return format;
    }

    public static Format getIntegerFormat(){
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(0);
        format.setGroupingUsed(false);
        return format;
    }

}
