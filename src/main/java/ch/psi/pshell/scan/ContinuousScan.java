package ch.psi.pshell.scan;

import ch.psi.pshell.device.ControlledSpeedable;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.device.Speedable;
import ch.psi.pshell.device.Writable;
import ch.psi.utils.Chrono;
import java.io.IOException;

/**
 *
 */
public abstract class ContinuousScan extends ScanBase {

    public class ContinuousScanFollowingErrorException extends IOException {

        ContinuousScanFollowingErrorException() {
            super("Position following error on continuous scan");
        }
    }

    final Double time;

    public ContinuousScan(Readable[] readables, double start, double end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[0], readables, new double[]{start}, new double[]{end}, new int[]{numberOfSteps}, relative, latency, passes, zigzag);
        time = null;
    }

    public ContinuousScan(Readable[] readables, double start, double end, double stepSize,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[0], readables, new double[]{start}, new double[]{end}, new double[]{stepSize}, relative, latency, passes, zigzag);
        time = null;
    }

    public ContinuousScan(Speedable writable, Readable[] readables, double start, double end, int numberOfSteps,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, new double[]{start}, new double[]{end}, new int[]{numberOfSteps}, relative, latency, passes, zigzag);
        time = null;
    }

    public ContinuousScan(Speedable writable, Readable[] readables, double start, double end, double stepSize,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(new Writable[]{writable}, readables, new double[]{start}, new double[]{end}, new double[]{stepSize}, relative, latency, passes, zigzag);
        time = null;
    }

    public ContinuousScan(ControlledSpeedable[] writable, Readable[] readables, double[] start, double[] end, int numberOfSteps, double time,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(getWritables(writable), readables, start, end, new int[]{numberOfSteps}, relative, latency, passes, zigzag);
        this.time = time;
    }

    public ContinuousScan(ControlledSpeedable[] writable, Readable[] readables, double[] start, double[] end, double stepSize[], double time,
            boolean relative, int latency, int passes, boolean zigzag) {
        super(getWritables(writable), readables, start, end, stepSize, relative, latency, passes, zigzag);
        this.time = time;
    }
    
    
    //This is just a hack for JEP interpreter have access to other prototypes
    public static class ContinuousScanStepSize extends ContinuousScan{
        public ContinuousScanStepSize(ControlledSpeedable[] writable, Readable[] readables, double[] start, double[] end, double stepSize[], double time,
                boolean relative, int latency, int passes, boolean zigzag) {
            super(writable, readables, start, end, stepSize, time, relative, latency, passes, zigzag);
        }
    }
    
    public static class ContinuousScanNumSteps extends ContinuousScan{
        public ContinuousScanNumSteps(ControlledSpeedable[] writable, Readable[] readables, double[] start, double[] end, int numberOfSteps, double time,
                boolean relative, int latency, int passes, boolean zigzag) {
            super(writable, readables, start, end, numberOfSteps, time, relative, latency, passes, zigzag);
        }
    }
        
    public static class ContinuousScanSingle extends ContinuousScan{
        public ContinuousScanSingle(Speedable writable, Readable[] readables, double start, double end, int numberOfSteps,
                boolean relative, int latency, int passes, boolean zigzag) {
            super(writable, readables, start, end, numberOfSteps, relative, latency, passes, zigzag);
        }
    }    

    public static Writable[] getWritables(ControlledSpeedable[] cs) {
        Writable[] ret = new Writable[cs.length];
        System.arraycopy(cs, 0, ret, 0, cs.length);
        return ret;
    } 
            
    public Speedable getSpeedable(int index) {
        return (Speedable) getWritables()[index];
    }

    public double getSpeed(int index) throws IOException, InterruptedException {
        return getSpeedable(index).getSpeed();
    }

    public double getScanTime() throws IOException, InterruptedException {
        if (time != null) {
            return time;
        }
        return Math.abs(getStart()[0] - getEnd()[0]) / getSpeed(0);
    }

    public double getStepTime() throws IOException, InterruptedException {
        return getScanTime() / getNumberOfSteps()[0];
    }

    protected void onBeforeScan() throws IOException, InterruptedException {
        if (time != null) {
            for (int i = 0; i < getWritables().length; i++) {
                ControlledSpeedable dev = (ControlledSpeedable) getWritables()[i];
                double speed = Math.abs(getStart()[i] - getEnd()[i]) / time;
                dev.assertValidSpeed(speed);
                dev.setSpeed(speed);
            }
        }
    }

    protected void onAfterScan() throws IOException, InterruptedException {
        if (time != null) {
            for (int i = 0; i < getWritables().length; i++) {
                ControlledSpeedable dev = (ControlledSpeedable) getWritables()[i];
                try {
                    dev.setSpeed(dev.getDefaultSpeed());
                } catch (IOException ex) {
                    logger.warning("Error restoring speed: " + dev.getName());
                }
            }
        }
    }

    @Override
    protected void doScan() throws IOException, InterruptedException {
        boolean backwards = isCurrentPassBackwards();
        if (backwards) {
            this.moveToEnd();
        } else {
            moveToStart();
        }

        int stepTimeMs = (int) (getStepTime() * 1000);

        for (int i = 0; i < getWritables().length; i++) {
            getWritables()[i].write(backwards ? getStart()[i] : getEnd()[i]);
        }

        for (int i = 0; i <= getNumberOfSteps()[0]; i++) {
            Chrono chrono = new Chrono();
            double[] pos = getWritablesPositions(i);
            processPosition(pos);
            if (i < (getNumberOfSteps()[0])) {
                if (!chrono.waitTimeout(stepTimeMs)) {
                    if (getCheckPositions()){
                        throw new ContinuousScanFollowingErrorException();
                    }
                }
            }
            if (!getCheckPositions()){
                if (getSpeedable(0).isReady()){
                    break;
                }
            }
        }
    }

    @Override
    protected void applyPosition(double[] position) throws IOException, InterruptedException {
        //Async move
    }

    @Override
    public int getDimensions() {
        return 1; //Writables move together;
    }
}
