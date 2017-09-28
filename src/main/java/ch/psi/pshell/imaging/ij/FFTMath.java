/*
 * ImageJ class customization to allow parametrization from embedded code
 */
package ch.psi.pshell.imaging.ij;

import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.measure.Calibration;
import java.awt.*;
import java.io.*;

/**
 * The class implements the Process/FFT/Math command.
 */
public class FFTMath implements PlugIn {

    private static final int CONJUGATE_MULTIPLY = 0, MULTIPLY = 1, DIVIDE = 2;

    /*
    private static String[] ops = {"Correlate", "Convolve", "Deconvolve"};
    private static int index1;
    private static int index2;
    private static int operation = CONJUGATE_MULTIPLY;
    private static boolean doInverse = true;
    private static String title = "Result";
    private ImagePlus imp1, imp2;
     */
    @Override
    public void run(String string) {
    }

    private static String title = "Result";

    public ImagePlus doMath(ImagePlus imp1, ImagePlus imp2, int operation, boolean doInverse) {
        FHT h1, h2 = null;
        ImageProcessor fht1, fht2;
        fht1 = (ImageProcessor) imp1.getProperty("FHT");
        if (fht1 != null) {
            h1 = new FHT(fht1);
        } else {
            IJ.showStatus("Converting to float");
            ImageProcessor ip1 = imp1.getProcessor();
            h1 = new FHT(ip1);
        }
        fht2 = (ImageProcessor) imp2.getProperty("FHT");
        if (fht2 != null) {
            h2 = new FHT(fht2);
        } else {
            ImageProcessor ip2 = imp2.getProcessor();
            if (imp2 != imp1) {
                h2 = new FHT(ip2);
            }
        }
        if (!h1.powerOf2Size()) {
            IJ.error("FFT Math", "Images must be a power of 2 size (256x256, 512x512, etc.)");
            return null;
        }
        if (imp1.getWidth() != imp2.getWidth()) {
            IJ.error("FFT Math", "Images must be the same size");
            return null;
        }
        if (fht1 == null) {
            IJ.showStatus("Transform image1");
            h1.transform();
        }
        if (fht2 == null) {
            if (h2 == null) {
                h2 = new FHT(h1.duplicate());
            } else {
                IJ.showStatus("Transform image2");
                h2.transform();
            }
        }
        FHT result = null;
        switch (operation) {
            case CONJUGATE_MULTIPLY:
                IJ.showStatus("Complex conjugate multiply");
                result = h1.conjugateMultiply(h2);
                break;
            case MULTIPLY:
                IJ.showStatus("Fourier domain multiply");
                result = h1.multiply(h2);
                break;
            case DIVIDE:
                IJ.showStatus("Fourier domain divide");
                result = h1.divide(h2);
                break;
        }
        ImagePlus imp3 = null;
        if (doInverse) {
            IJ.showStatus("Inverse transform");
            result.inverseTransform();
            IJ.showStatus("Swap quadrants");
            result.swapQuadrants();
            IJ.showStatus("Display image");
            result.resetMinAndMax();
            imp3 = new ImagePlus(title, result);
        } else {
            IJ.showStatus("Power spectrum");
            ImageProcessor ps = result.getPowerSpectrum();
            imp3 = new ImagePlus(title, ps.convertToFloat());
            result.quadrantSwapNeeded = true;
            imp3.setProperty("FHT", result);
        }
        Calibration cal1 = imp1.getCalibration();
        Calibration cal2 = imp2.getCalibration();
        Calibration cal3 = cal1.scaled() ? cal1 : cal2;
        if (cal1.scaled() && cal2.scaled() && !cal1.equals(cal2)) {
            cal3 = null;                //can't decide between different calibrations
        }
        imp3.setCalibration(cal3);
        cal3 = imp3.getCalibration();   //imp3 has a copy, which we may modify
        cal3.disableDensityCalibration();
        //imp3.show();
        IJ.showProgress(1.0);
        return imp3;
    }
}
