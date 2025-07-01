package ch.psi.pshell.scan;

import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Speedable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.logging.Level;

/**
 * Continuous scan managed by hardware: values are received asynchronously
 */
public abstract class HardwareScan extends ContinuousScan {

    final Map<String, Object> configuration;

    protected HardwareScan(Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, int numberOfSteps, int passes, boolean zigzag) {
        this(configuration, writable, readables, start, end, (Math.abs(end - start) / numberOfSteps), passes, zigzag);
    }

    protected HardwareScan(Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, double stepSize, int passes, boolean zigzag) {
        super(writable, readables, start, end, stepSize, false, 0, passes, zigzag);
        this.configuration = configuration;
    }

    public static HardwareScan newScan(Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, int numberOfSteps, int passes, boolean zigzag) throws Exception {
        double stepSize = Math.abs(end - start) / numberOfSteps;
        return newScan(configuration, writable, readables, start, end, stepSize, passes, zigzag);
    }

    public static HardwareScan newScan(Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, double stepSize, int passes, boolean zigzag) throws Exception {
        Class cls = Class.forName((String) configuration.get("class"));
        return newScan(cls, configuration, writable, readables, start, end, stepSize, passes, zigzag);
    }

    public static HardwareScan newScan(Class cls, Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, int numberOfSteps, int passes, boolean zigzag) throws Exception {
        double stepSize = Math.abs(end - start) / numberOfSteps;
        return newScan(cls, configuration, writable, readables, start, end, stepSize, passes, zigzag);
    }

    public static HardwareScan newScan(Class cls, Map<String, Object> configuration, Speedable writable, Readable[] readables, double start, double end, double stepSize, int passes, boolean zigzag) throws Exception {
        Constructor c = cls.getConstructor(new Class[]{Map.class, Speedable.class, Readable[].class, double.class, double.class, double.class, int.class, boolean.class});
        return (HardwareScan) c.newInstance(new Object[]{configuration, writable, readables, start, end, stepSize, passes, zigzag});
    }

    double passStart;
    double passEnd;

    protected double getPassStart() {
        return passStart;
    }

    protected double getPassEnd() {
        return passEnd;
    }

    protected boolean isPositiveDirection() {
        return passStart <= passEnd;
    }

    protected void onBeforeScan() throws IOException, InterruptedException {
        try {
            prepare();
        } catch (Exception ex) {
            try {
                cleanup();
            } catch (Exception e) {
                logger.log(Level.WARNING, null, e);
            }
            throw ex;
        }
    }

    protected void onAfterScan() throws IOException, InterruptedException {
        try {
            cleanup();
        } catch (Exception ex) {
            logger.log(Level.WARNING, null, ex);
        }
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        boolean backwards = isCurrentPassBackwards();
        passStart = backwards ? getEnd()[0] : getStart()[0];
        passEnd = backwards ? getStart()[0] : getEnd()[0];
        try {
            execute();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    @Override
    protected void setPosition(double[] position) throws IOException, InterruptedException {
        //Don't initialize the position directly
    }

    Map<String, Object> getParameters() {
        return configuration;
    }

    protected abstract void prepare() throws IOException, InterruptedException;

    protected abstract void execute() throws Exception;

    protected abstract void cleanup() throws IOException, InterruptedException;

    protected void onBeforeStream(int pass) {
    }

    protected void onAfterStream(int pass) {
    }
}
