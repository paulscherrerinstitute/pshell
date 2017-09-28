package ch.psi.pshell.epics;

import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Readable;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public class Camtool extends ArraySource {

    final String prefix;
    final String dataPrefix;
    final boolean latch;
    final Object lock = new Object();

    final public ChannelInteger channelRun;
    final public ChannelInteger channelLatch;
    final public ChannelDouble channelTimestamp;
    final public ChannelDoubleArray origin;
    final public ChannelDoubleArray profileX, profileY;
    final public ChannelIntegerArray shape;
    final public ChannelInteger roiEnabled;
    final public ChannelIntegerArray roi;
    final public ChannelInteger bgEnable, bgCapture, bgCaptureRemain;
    final public ChannelDouble calOffX, calOffY, calScaleX, calScaleY;
    final public ChannelDouble posX, posY;
    final public ChannelDoubleArray gaussX, gaussY;
    final public ChannelDouble gaussCenterX, gaussCenterY;
    final public ChannelDouble gaussStdDivX, gaussStdDivY;

    final public CamToolPosX posMeanX;
    final public CamToolPosY posMeanY;
    final public CamToolVarX posVarX;
    final public CamToolVarY posVarY;

    public Camtool(String name, String prefix) {
        this(name, prefix, false);
    }

    public Camtool(String name, String prefix, boolean latch) {
        this(name, prefix, latch, false);
    }

    public Camtool(String name, String prefix, boolean latch, boolean gr) {
        super(name, prefix + (latch ? ":latch" : ":pipeline") + ".roi.output");
        this.prefix = prefix + ":";
        this.latch = latch;
        dataPrefix = this.prefix + (latch ? "latch" : "pipeline") + ".";

        channelRun = new ChannelInteger(name + " run", this.prefix + "camera.run");
        channelLatch = new ChannelInteger(name + " latch", this.prefix + "latch.capture");
        channelTimestamp = new ChannelDouble(name + " timestamp", dataPrefix + "timestamp");
        channelTimestamp.setMonitored(true);
        profileX = new ChannelDoubleArray(name + " profile x", dataPrefix + "profile.x");
        profileY = new ChannelDoubleArray(name + " profile y", dataPrefix + "profile.y");
        shape = new ChannelIntegerArray(name + " shape", dataPrefix + ("roi.output.shape"));
        roiEnabled = new ChannelInteger(name + " roi enable", dataPrefix + ("roi.enabled"));
        roi = new ChannelIntegerArray(name + " roi", dataPrefix + ("roi.roi"));
        origin = new ChannelDoubleArray(name + " origin X", dataPrefix + "roi.origin_out");
        bgEnable = new ChannelInteger(name + " bg enable", this.prefix + "pipeline.bg.enabled");
        bgCapture = new ChannelInteger(name + " bg capture", this.prefix + "pipeline.bg.capture");
        bgCaptureRemain = new ChannelInteger(name + " bg capture remain", this.prefix + "pipeline.bg.capture_remain");
        calOffX = new ChannelDouble(name + " cal off x", this.prefix + "pipeline.egu.eoff_x");
        calOffY = new ChannelDouble(name + " cal off y", this.prefix + "pipeline.egu.eoff_y");
        calScaleX = new ChannelDouble(name + " cal scale x", this.prefix + "pipeline.egu.eslo_x");
        calScaleY = new ChannelDouble(name + " cal scale y", this.prefix + "pipeline.egu.eslo_y");

        String x_stats = dataPrefix + "x_stats" + (gr ? "_gr" : "") + ".";
        String y_stats = dataPrefix + "y_stats" + (gr ? "_gr" : "") + ".";

        posX = new ChannelDouble(name + " com x", x_stats + "com_egu");
        posY = new ChannelDouble(name + " com y", y_stats + "com_egu");
        gaussX = new ChannelDoubleArray(name + " gauss x", x_stats + "gauss");
        gaussY = new ChannelDoubleArray(name + " gauss y", y_stats + "gauss");
        gaussCenterX = new ChannelDouble(name + "gauss center x", x_stats + "g_center_egu");
        gaussCenterY = new ChannelDouble(name + "gauss center y", y_stats + "g_center_egu");
        gaussStdDivX = new ChannelDouble(name + "gauss stddiv x", x_stats + "g_stddiv_egu");
        gaussStdDivY = new ChannelDouble(name + "gauss stddiv t", y_stats + "g_stddiv_egu");

        posMeanX = new CamToolPosX();
        posMeanY = new CamToolPosY();
        posVarX = new CamToolVarX();
        posVarY = new CamToolVarY();
    }

    @Override
    public void doSetMonitored(boolean value) {
        super.doSetMonitored(value);
        getDevice().setMonitored(value);
    }

    void safeInitialize(Device dev, int timeout) throws IOException, InterruptedException {
        for (int retries = 0; retries < 10; retries++) {
            try {
                dev.initialize();
                break;
            } catch (IOException ex) {
                if (retries == 9) {
                    throw ex;
                }
                Thread.sleep(timeout / 10);
            }
        }
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        try {
            channelRun.initialize();
            channelLatch.initialize();
            if (latch) {
                start();
                latch();
            }
            safeInitialize(channelTimestamp, 2000);
            posX.initialize();
            posY.initialize();
            profileX.initialize();
            profileY.initialize();
            shape.initialize();
            roiEnabled.initialize();
            roi.initialize();
            origin.initialize();
            bgEnable.initialize();
            bgCapture.initialize();
            bgCaptureRemain.initialize();
            try {
                calOffX.initialize();
                calOffY.initialize();
                calScaleX.initialize();
                calScaleY.initialize();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            gaussX.initialize();
            gaussY.initialize();
            gaussCenterX.initialize();
            gaussCenterY.initialize();
            gaussStdDivX.initialize();
            gaussStdDivY.initialize();

            int[] s = shape.read();
            getConfig().imageHeight = s[0];
            getConfig().imageWidth = s[1];
            getConfig().save();
            getDevice().setSize(getConfig().imageHeight * getConfig().imageWidth);
            super.doInitialize();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        channelRun.close();
        channelLatch.close();
        channelTimestamp.close();
        posX.close();
        posY.close();
        profileX.close();
        profileY.close();
        shape.close();
        roiEnabled.close();
        roi.close();
        origin.close();
        bgEnable.close();
        bgCapture.close();
        bgCaptureRemain.close();
        calOffX.close();
        calOffY.close();
        calScaleX.close();
        calScaleY.close();
        gaussX.close();
        gaussY.close();
        gaussCenterX.close();
        gaussCenterY.close();
        gaussStdDivX.close();
        gaussStdDivY.close();
        gaussX.close();
        gaussY.close();
        gaussCenterX.close();
        gaussCenterY.close();
        gaussStdDivX.close();
        gaussStdDivY.close();
    }

    int numImages = 1;

    public int getNumImages() {
        return numImages;
    }

    public void setNumImages(int value) {
        numImages = value;
    }

    double grabTimeout = 3.0;

    public double getGrabTimeout() {
        return grabTimeout;
    }

    public void setGrabTimeou(double value) {
        grabTimeout = value;
    }

    public void capture() throws IOException, InterruptedException {
        int retries = 3;
        while (true) {
            try {
                double timestamp = channelTimestamp.read();
                if (latch) {
                    channelLatch.write(1);
                } else {
                    channelRun.write(1);
                }
                long start = System.currentTimeMillis();
                while (true) {
                    double val = channelTimestamp.read();
                    if (timestamp != val) {
                        return;
                    }
                    if ((System.currentTimeMillis() - start) > grabTimeout) {
                        throw new IOException("Frame timeout");
                    }
                    Thread.sleep(5);
                }
            } catch (IOException ex) {
                retries--;
                if (--retries <= 0) {
                    throw ex;
                }
            }
        }
    }

    public void start() throws IOException, InterruptedException {
        channelRun.write(-1);
    }

    public void stop() throws IOException, InterruptedException {
        channelRun.write(0);
    }

    public void grabSingle() throws IOException, InterruptedException {
        channelRun.write(1);
    }

    public void latch() throws IOException, InterruptedException {
        channelLatch.write(1);
    }

    public void enableBackground(boolean value) throws IOException, InterruptedException {
        bgEnable.write(value ? 1 : 0);
    }

    public void captureBackground(int images) throws IOException, InterruptedException {
        start();
        bgCapture.write(images);
        Thread.sleep(200);
        while (bgCaptureRemain.read() > 0) {
            Thread.sleep(10);
        }
    }

    //Statisticss pseudo devices
    ArrayList<Double> posXSamples;
    ArrayList<Double> posYSamples;

    public void updateStats() throws IOException, InterruptedException {
        posXSamples.clear();
        posYSamples.clear();
        for (int i = 0; i < getNumImages(); i++) {
            capture();
            posXSamples.add(posX.read());
            posXSamples.add(posY.read());
        }
    }

    class CamToolPosX implements Readable {

        @Override
        public Object read() throws IOException, InterruptedException {
            return mean(posXSamples);
        }

    }

    class CamToolPosY implements Readable {

        @Override
        public Object read() throws IOException, InterruptedException {
            return mean(posYSamples);
        }
    }

    class CamToolVarX implements Readable {

        @Override
        public Object read() throws IOException, InterruptedException {
            return stdev(posXSamples);
        }

    }

    class CamToolVarY implements Readable {

        @Override
        public Object read() throws IOException, InterruptedException {
            return stdev(posYSamples);
        }
    }

    public double mean(ArrayList<Double> samples) {
        int count = 0;
        double temp = 0;
        for (Double n : samples) {
            if (!Double.isNaN(n)) {
                temp += n.doubleValue();
                count++;
            }
        }
        return count == 0 ? Double.NaN : temp / count;
    }

    public double stdev(ArrayList<Double> samples) {
        int count = 0;
        double temp = 0;
        double mean = mean(samples);
        for (Double n : samples) {
            if (!Double.isNaN(n)) {
                temp += Math.pow((mean - n), 2);
            }
        }
        return count == 0 ? Double.NaN : temp / count;
    }
}
