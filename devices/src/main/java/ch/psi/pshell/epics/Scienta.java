package ch.psi.pshell.epics;

import ch.psi.pshell.device.AccessType;
import ch.psi.pshell.device.ArrayCalibration;
import ch.psi.pshell.device.CameraImageDescriptor;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.MatrixCalibration;
import ch.psi.pshell.device.Readable.ReadableCalibratedArray;
import ch.psi.pshell.utils.Arr;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Scienta spectrometer analyser.
 */
public class Scienta extends AreaDetector {

    final ChannelInteger slices, frames;
    final ChannelDouble lowEnergy, centerEnergy, highEnergy, stepSize, stepTime, energyWidth;
    final ChannelDoubleArray spectrum, spectrumX, image, extio;
    final ChannelDouble currentChannel, totalPoints;
    final ChannelDouble channelBegin, channelEnd, sliceBegin, sliceEnd;
    final ChannelString lensMode, acquisitionMode, passEnergy;
    final ChannelInteger numChannels, numSlices;
    final ChannelDouble acquisitionTime;
    final Stats[] stats;

    public Scienta(final String name, final String channelPrefix) {
        this(name, channelPrefix + ":cam1", channelPrefix + ":image1");
    }

    public Scienta(String name, String channelCtrl, String channelData) {
        super(name, channelCtrl, channelData);
        slices = new ChannelInteger(name + " slices", channelCtrl + ":SLICES", false);
        frames = new ChannelInteger(name + " frames", channelCtrl + ":FRAMES", false);

        lowEnergy = new ChannelDouble(name + " low energy", channelCtrl + ":LOW_ENERGY", 3, false);
        centerEnergy = new ChannelDouble(name + " center energy", channelCtrl + ":CENTRE_ENERGY", 3, false);
        highEnergy = new ChannelDouble(name + " high energy", channelCtrl + ":HIGH_ENERGY", 3, false);
        stepSize = new ChannelDouble(name + " step size", channelCtrl + ":STEP_SIZE", 3, false);
        stepTime = new ChannelDouble(name + " step time", channelCtrl + ":STEP_TIME", 3, false);

        energyWidth = new ChannelDouble(name + " energy width", channelCtrl + ":ENERGY_WIDTH_RBV", 3, false);
        energyWidth.setAccessType(AccessType.Read);
        passEnergy = new ChannelString(name + " pass energy", channelCtrl + ":PASS_ENERGY", false);
        lensMode = new ChannelString(name + " lens mode", channelCtrl + ":LENS_MODE", false);
        acquisitionMode = new ChannelString(name + " lens mode", channelCtrl + ":ACQ_MODE", false);
        channelBegin = new ChannelDouble(name + " channel begin", channelCtrl + ":CHANNEL_BEGIN_RBV", 3, false);
        channelBegin.setAccessType(AccessType.Read);
        channelEnd = new ChannelDouble(name + " channel end", channelCtrl + ":CHANNEL_END_RBV", 3, false);
        channelEnd.setAccessType(AccessType.Read);
        sliceBegin = new ChannelDouble(name + " slice begin", channelCtrl + ":SLICE_BEGIN_RBV", 3, false);
        sliceBegin.setAccessType(AccessType.Read);
        sliceEnd = new ChannelDouble(name + " slice end", channelCtrl + ":SLICE_END_RBV", 3, false);
        sliceEnd.setAccessType(AccessType.Read);
        numChannels = new ChannelInteger(name + " num channels", channelCtrl + ":NUM_CHANNELS_RBV", false);
        numChannels.setAccessType(AccessType.Read);
        numSlices = new ChannelInteger(name + " num slices", channelCtrl + ":SLICES_RBV", false);
        numSlices.setAccessType(AccessType.Read);
        acquisitionTime = new ChannelDouble(name + " acquire time", channelCtrl + ":TOTAL_ACQ_TIME_RBV", 3, false);
        acquisitionTime.setAccessType(AccessType.Read);

        spectrum = new ScientaSpectrum();
        spectrumX = new ChannelDoubleArray(Scienta.this.getName() + " spectrum x", channelCtrl + ":CHANNEL_SCALE_RBV");
        image = new ChannelDoubleArray(name + " image", channelCtrl + ":IMAGE", 8, EpicsRegister.SIZE_MAX, false);
        image.setAccessType(AccessType.Read);
        extio = new ChannelDoubleArray(name + " extio", channelCtrl + ":EXTIO", 8, EpicsRegister.SIZE_MAX, false);
        extio.setAccessType(AccessType.Read);

        currentChannel = new ChannelDouble(name + " current channel", channelCtrl + ":CURRENT_CHANNEL_RBV", 0, false);
        currentChannel.setAccessType(AccessType.Read);
        totalPoints = new ChannelDouble(name + " total points", channelCtrl + ":TOTAL_POINTS_RBV", 0, false);
        totalPoints.setAccessType(AccessType.Read);

        addChildren(new Device[]{slices, frames,
            lowEnergy, centerEnergy, highEnergy, stepSize, stepTime, energyWidth,
            spectrum, spectrumX, image, extio,
            currentChannel, totalPoints,
            channelBegin, channelEnd, sliceBegin, sliceEnd,
            passEnergy, lensMode, acquisitionMode,
            numChannels, numSlices, acquisitionTime
        });

        stats = new Stats[5];
        stats[0] = new Stats("CountsR1", 1);
        stats[1] = new Stats("CountsR2", 2);
        stats[2] = new Stats("CountsR3", 3);
        stats[3] = new Stats("CountsR4", 4);
        stats[4] = new Stats("Counts", 5);
        addChildren(stats);
    }

    public class ScientaSpectrum extends ChannelDoubleArray implements ReadableCalibratedArray<double[]> {

        ScientaSpectrum() {
            super(Scienta.this.getName() + " spectrum", getChannelCtrl() + ":INT_SPECTRUM", 8, 200, false);
            setAccessType(AccessType.Read);
        }

        @Override
        protected double[] doRead() throws IOException, InterruptedException {
            setSizeToValidElements();
            return super.doRead();
        }

        @Override
        public ArrayCalibration getCalibration() {
            double scale = 1.0;
            double offset = 0.0;
            try {
                List<Double> channelRange = getChannelRange();
                Double cb = channelRange.get(0);
                Double ce = channelRange.get(1);
                setSizeToValidElements();
                scale = (ce - cb) / Math.max(getSize() - 1, 1);
                offset = cb;
            } catch (Exception ex) {
            }
            return new ArrayCalibration(scale, offset);
        }
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        setCache(channelBegin, 50.0);
        setCache(channelEnd, 150.0);
        setCache(sliceBegin, -20.0);
        setCache(sliceEnd, 20.0);
        setCache(numChannels, 100);
        setCache(numSlices, 10);
        setCache(spectrum, new double[100]);
        setCache(spectrumX, Arr.indexesDouble(100));
        setCache(currentChannel, 0.0);
        setCache(totalPoints, 100.0);
        setCache(passEnergy, String.valueOf(PASS_ENERGY_VALUES[0]));
        setCache(lensMode, LensMode.Angular45.toString());
        setCache(acquisitionMode, AcquisitionMode.Fixed.toString());
        setCache(acquisitionTime, 10.0);

        setSimulatedValue("ENERGY_MODE", EnergyMode.Binding.toString());
        setSimulatedValue("DETECTOR_MODE", DetectorMode.Pulse_Counting.toString());
        setSimulatedValue("ELEMENT_SET", ElementSet.High_Pass.toString());
        //setSimulatedValue("ACQ_MODE", AcquisitionMode.Fixed.toString());
        //setSimulatedValue("LENS_MODE", LensMode.Angular45.toString());
        //setSimulatedValue("PASS_ENERGY", String.valueOf(PASS_ENERGY_VALUES[0]));
    }
    
    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        //spectrum.setSize(ChannelDoubleArray.KEEP_TO_VALID);
        //spectrumX.setSize(ChannelDoubleArray.KEEP_TO_VALID);
    }
    

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        numChannels.update();
        currentChannel.update();
        slices.update();
        lowEnergy.update();
        centerEnergy.update();
        highEnergy.update();
        energyWidth.update();
        stepSize.update();
        stepTime.update();
        totalPoints.update();
        passEnergy.update();
        lensMode.update();
        acquisitionMode.update();
        acquisitionTime.update();
        channelBegin.update();
        channelEnd.update();
        sliceBegin.update();
        sliceEnd.update();
    }

    @Override
    protected void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        numChannels.setMonitored(value);
        currentChannel.setMonitored(value);
        slices.setMonitored(value);
        lowEnergy.setMonitored(value);
        centerEnergy.setMonitored(value);
        highEnergy.setMonitored(value);
        energyWidth.setMonitored(value);
        stepSize.setMonitored(value);
        stepTime.setMonitored(value);
        totalPoints.setMonitored(value);
        passEnergy.setMonitored(value);
        lensMode.setMonitored(value);
        acquisitionMode.setMonitored(value);
        acquisitionTime.setMonitored(value);
        channelBegin.setMonitored(value);
        channelEnd.setMonitored(value);
        sliceBegin.setMonitored(value);
        sliceEnd.setMonitored(value);
    }

    @Override
    protected CameraImageDescriptor doReadImageDescriptor() throws IOException, InterruptedException {
        CameraImageDescriptor ret = super.doReadImageDescriptor();
        try{
            List<Double> channelRange = getChannelRange();        
            Double cb = channelRange.get(0);
            Double ce = channelRange.get(1);

            List<Double> sliceRange = getSliceRange();
            Double sb = sliceRange.get(0);
            Double se = sliceRange.get(1);

            if ((cb == null) || (ce == null) || (sb == null) || (se == null) || (ret.width == 0) || (ret.height == 0)) {
                ret.calibration = null;
            } else {
                double scaleX = (ce - cb) / Math.max(ret.width - 1, 1);
                double offsetX = cb;
                double scaleY = (se - sb) / Math.max(ret.height - 1, 1);
                double offsetY = sb;

                ret.calibration = new MatrixCalibration(scaleX, scaleY, offsetX, offsetY);
            }
        } catch (Exception  ex){
            ret.calibration = null;
        }
        return ret;
    }

    //Modes
    public enum AcquisitionMode {

        Swept,
        Fixed
    }

    public void setAcquisitionMode(AcquisitionMode mode) throws IOException, InterruptedException {
        //writeCtrlEnum("ACQ_MODE", String.valueOf(mode));
        acquisitionMode.write(String.valueOf(mode));
    }

    public AcquisitionMode getAcquisitionMode() throws IOException, InterruptedException {
        //return (AcquisitionMode) readCtrlEnum("ACQ_MODE", AcquisitionMode.class);
        String val = acquisitionMode.getValue();
        return (AcquisitionMode) convertCtrlEnum(val, AcquisitionMode.class);
    }

    public enum EnergyMode {

        Binding,
        Kinetic
    }

    public void setEnergyMode(EnergyMode mode) throws IOException, InterruptedException {
        writeCtrlEnum("ENERGY_MODE", String.valueOf(mode));
    }

    public EnergyMode getEnergyMode() throws IOException, InterruptedException {
        return (EnergyMode) readCtrlEnum("ENERGY_MODE", EnergyMode.class);
    }

    public enum LensMode {

        Transmission,
        Angular45,
        Angular60
    }

    public void setLensMode(LensMode mode) throws IOException, InterruptedException {
        //writeCtrlEnum("LENS_MODE", String.valueOf(mode));
        lensMode.write(String.valueOf(mode));
    }

    public LensMode getLensMode() throws IOException, InterruptedException {
        //return (LensMode) readCtrlEnum("LENS_MODE", LensMode.class);
        String val = lensMode.getValue();
        return (LensMode) convertCtrlEnum(val, LensMode.class);
    }

    public enum DetectorMode {

        Pulse_Counting,
        ADC
    }

    public void setDetectorMode(DetectorMode mode) throws IOException, InterruptedException {
        writeCtrlEnum("DETECTOR_MODE", String.valueOf(mode));
    }

    public DetectorMode getDetectorMode() throws IOException, InterruptedException {
        return (DetectorMode) readCtrlEnum("DETECTOR_MODE", DetectorMode.class);
    }

    public enum ElementSet {

        Low_Pass,
        High_Pass
    }

    public void setElementSet(ElementSet mode) throws IOException, InterruptedException {
        writeCtrlEnum("ELEMENT_SET", String.valueOf(mode));
    }

    public ElementSet getElementSet() throws IOException, InterruptedException {
        return (ElementSet) readCtrlEnum("ELEMENT_SET", ElementSet.class);
    }

    public static final int[] PASS_ENERGY_VALUES = new int[]{2, 5, 10, 20, 50, 100, 200};

    public void setPassEnergy(int energy) throws IOException, InterruptedException {
        //writeCtrl("PASS_ENERGY", String.valueOf(energy));
        passEnergy.write(String.valueOf(energy));

    }

    public int getPassEnergy() throws IOException, InterruptedException {
        //String ret = (String) readCtrl("PASS_ENERGY", String.class);
        String ret = passEnergy.getValue();
        try {
            return Integer.valueOf(ret);
        } catch (Exception ex) {
            throw new DeviceInvalidParameterException("Pass Energy", ret);
        }
    }

    public void zeroSupplies() throws IOException, InterruptedException {
        writeCtrl("ZERO_SUPPLIES", 1);
    }

    //Progress    
    //Disconnected operations
    public double getProgress() {
        Double cur = currentChannel.take();
        Double total = totalPoints.take();
        if ((cur == null) || (total == null) || (total == 0)) {
            return 0.0;
        }
        return cur / total;
    }

    public double[] getSpectrumX() throws IOException, InterruptedException {
        spectrumX.setSizeToValidElements();
        return spectrumX.read();
    }

    //Direct register access
    public ChannelInteger getSlices() {
        return slices;
    }

    public ChannelInteger getFrames() {
        return frames;
    }

    public ChannelDouble getLowEnergy() {
        return lowEnergy;
    }

    public ChannelDouble getCenterEnergy() {
        return centerEnergy;
    }

    public ChannelDouble getHighEnergy() {
        return highEnergy;
    }

    public ChannelDouble getStepSize() {
        return stepSize;
    }

    public ChannelDouble getStepTime() {
        return stepTime;
    }

    public ChannelDouble getEnergyWidth() {
        return energyWidth;
    }

    public ChannelDoubleArray getSpectrum() {
        return spectrum;
    }

    public ChannelDoubleArray getSpectrumScale() {
        return spectrumX;
    }

    public ChannelDoubleArray getImage() {
        return image;
    }

    public ChannelDoubleArray getExtio() {
        return extio;
    }

    public ChannelDouble getChannelBegin() {
        return channelBegin;
    }

    public ChannelDouble getChannelEnd() {
        return channelEnd;
    }

    public ChannelDouble getSliceBegin() {
        return sliceBegin;
    }

    public ChannelDouble getSliceEnd() {
        return sliceEnd;
    }

    public ChannelDouble getAcquisitionTime() {
        return acquisitionTime;
    }

    public List<Double> getChannelRange() throws IOException, InterruptedException {
        ArrayList<Double> ret = new ArrayList<>();
        ret.add(getChannelBegin().getValue());
        ret.add(getChannelEnd().getValue());
        return ret;
    }

    public List<Double> getSliceRange() throws IOException, InterruptedException {
        ArrayList<Double> ret = new ArrayList<>();
        ret.add(sliceBegin.getValue());
        ret.add(sliceEnd.getValue()); 
        return ret;
    }

    public ChannelInteger getNumChannels() {
        return numChannels;
    }

    public ChannelInteger getNumSlices() {
        return numSlices;
    }

    public ChannelDouble getTotalChannels() {
        return totalPoints;
    }

    public ChannelDouble getCurrentChannel() {
        return currentChannel;
    }

    public Stats[] getStats() {
        return stats;
    }

    public class Stats extends ChannelDouble {

        final int index;
        final ChannelInteger uid;

        Stats(String name, int index) {
            super(name, getChannelCtrl().split(":")[0] + ":Stats" + index + ":Total_RBV", 3, false);
            this.index = index;
            uid = new ChannelInteger(name + " uid", getChannelCtrl().split(":")[0] + ":Stats" + index + ":UniqueId_RBV", false);
            //setParent(Scienta.this);
            addChild(uid);
        }

        @Override
        public boolean isReady() throws IOException, InterruptedException {
            Integer imageCounter = getImageCounter().getValue();
            if (imageCounter == null) {
                return false;
            }
            Integer id = uid.take();
            if ((id == null) || (!imageCounter.equals(id))) {
                uid.update();
            }
            return imageCounter.equals(uid.take());
        }

        @Override
        public Double read() throws IOException, InterruptedException {
            assertInitialized();
            waitReady(10000);
            return super.read();
        }

        public int getUID() throws IOException, InterruptedException {
            return uid.getValue();
        }
    }
}
