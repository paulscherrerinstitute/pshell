package ch.psi.pshell.crlogic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Readable;
import ch.psi.pshell.epics.Epics;
import ch.psi.pshell.scan.HardwareScan;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Implement a HardwareScan using Crlogic (OTF scan using ZMQ streaming in Epics IOCs).
 */
public class CrlogicScan extends HardwareScan {

    private static final Logger logger = Logger.getLogger(CrlogicScan.class.getName());

    public static final int HIGH_WATER_MARK = 100;
    private Context context;
    private Socket socket;
    private final ObjectMapper mapper = new ObjectMapper();

    // Default timeout (in milliseconds) for wait operations
    private final long startStopTimeout = 8000;

    // Prefix for the CRLOGIC channels
    private TemplateCrlogic template;
    private List<String> readoutResources;
    private final AtomicBoolean executing = new AtomicBoolean(false);

    final CrlogicPositioner positioner;

    final ArrayList<CrlogicSensor> sensors;
    final ArrayList<Readable> genericSensors;

    final String prefix;
    final String ioc;
    final double integrationTime;
    final double additionalBacklash;
    final double stepSize;

    double extEncoderOffset;

    public CrlogicScan(Map<String, Object> configuration, CrlogicPositioner writable, Readable[] readables, double start, double end, double stepSize, int passes, boolean zigzag) {
        super(configuration, writable, readables, start, end, stepSize, passes, zigzag);
        this.positioner = writable;
        this.readoutResources = new ArrayList<>();
        sensors = new ArrayList<>();
        genericSensors = new ArrayList<>();
        this.stepSize = stepSize;

        prefix = (String) configuration.get("prefix");
        ioc = (String) configuration.get("ioc");
        integrationTime = configuration.containsKey("integrationTime") ? (Double) configuration.get("integrationTime") : 0.01;
        additionalBacklash = configuration.containsKey("additionalBacklash") ? (Double) configuration.get("additionalBacklash") : 0.0;
        for (Readable r : readables) {
            if (r instanceof CrlogicSensor) {
                sensors.add((CrlogicSensor) r);
            } else {
                genericSensors.add(r);
            }
        }
    }

    /**
     * Receive a ZMQ message
     */
    private void receive() throws IOException, InterruptedException {

        MainHeader mainHeader;

        try {
            byte[] bytes = socket.recv(ZMQ.NOBLOCK);
            if (bytes == null) {
                // There is no message hanging
                return;
            }
            mainHeader = mapper.readValue(bytes, MainHeader.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to decode main header", e);
        }

        if (!socket.hasReceiveMore()) {
            throw new RuntimeException("There is no data submessage");
        }

        byte[] bytes = socket.recv();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

//        List<Object> data = new ArrayList<>();
        boolean use = true;
        double pos = Double.NaN;

        for (int i = 0; i < mainHeader.getElements(); i++) {
            Double raw = byteBuffer.getDouble();
//            Double val;

            if (i == 0) {
                ((CrlogicPositioner.ReadbackRegister) positioner.getReadback()).setRawValue(raw);
                pos = positioner.getReadback().take();
                use = isInRange(pos);
//                data.add(pos);
            } else {
                CrlogicSensor sensor = sensors.get(i - 1);
                sensor.setRawValue(raw);
//                val = sensor.take();
//                data.add(val);
            }

        }

        // Drain remaining messages
        int n = drainHangingSubmessages();
        if (n > 1) {
            throw new RuntimeException("More than 1 message drained from stream: " + n);
        }

        if (use) {
            //System.out.println(String.join(", ", Convert.toStringArray(data.toArray())));            
            processPosition(new double[]{pos});
            //listener.onData(data);
        }
    }

    private boolean wasEqualBefore = false;

    public boolean isInRange(Double value) {
        double start = getPassStart();
        double end = getPassEnd();
        if (positioner.getUseExtEncoder()) {
            value = value - extEncoderOffset;
        }

        if (this.isPositiveDirection()) {
            if (start <= value && value <= end) {

                // If motor is very accurete and user backlash==null it might be that value is exactly 
                // the end value. To prevent that unnecessary data is captured execute this check
                if (wasEqualBefore) {
                    wasEqualBefore = false; // Reset flag
                    return false;
                }

                // Check whether was equal
                if (value == end) {
                    wasEqualBefore = true;
                }

                return true;
            } else {
                return false;
            }
        } else {
            if (end <= value && value <= start) {
                if (wasEqualBefore) {
                    wasEqualBefore = false; // Reset flag
                    return false;
                }
                // Check whether was equal
                if (value == start) {
                    wasEqualBefore = true;
                }
                return true;
            } else {
                return false;
            }
        }
    }

//    @Override
    protected void execute() throws Exception {
        // Set abort state to false
        executing.set(true);
        double start = getPassStart();
        double end = getPassEnd();
        int timeout = 600000; // 10 minutes move timeout

        // Check if logic is inactive, otherwise return early
        if (!template.getStatus().getValue().equals(TemplateCrlogic.Status.INACTIVE.toString())) {
            logger.info("CRLOGIC is not inactive!");
            // TODO Decide what to do in this situation
            String status = template.getStatus().getValue();
            if (status.equals(TemplateCrlogic.Status.FAULT.toString())) {
                // If in fault show message and recover
                logger.info("CRLOGIC in FAULT state");
                logger.info("Error message: " + template.getMessage().getValue());
                logger.info("Recover logic and set it to INACTIVE");
                template.getStatus().setValue(TemplateCrlogic.Status.INACTIVE.toString());
            } else if (status.equals(TemplateCrlogic.Status.ACTIVE.toString())) {
                template.getStatus().setValue(TemplateCrlogic.Status.STOP.toString());
                template.getStatus().waitForValue(TemplateCrlogic.Status.INACTIVE.toString(), startStopTimeout);
            } else {
                throw new RuntimeException("CRLOGIC is not inactive");
            }
        }

        logger.info("Set parameters");
        template.getNfsServer().setValue("");
        template.getNfsShare().setValue("");
        template.getDataFile().setValue("");

        int tps = template.getTicksPerSecond().getValue();
        logger.info("Ticks per second: " + tps);

        logger.info("Set readout resources");

        template.getReadoutResources().setValue(readoutResources.toArray(new String[readoutResources.size()]));

        // Set ticks between interrupt to integration time
        int ticks = (int) (tps * integrationTime);
        template.getTicksBetweenInterrupts().setValue(ticks);

        // Prepare motor
        double totalTimeSeconds = Math.abs((end - start) / stepSize * integrationTime);
        int hours = (int) Math.floor(totalTimeSeconds / 60 / 60);
        int minutes = (int) Math.floor(totalTimeSeconds / 60 - hours * 60);
        int seconds = (int) Math.floor(totalTimeSeconds - hours * 60 * 60 - minutes * 60);

        logger.info("Estimated time: " + hours + ":" + minutes + ":" + seconds);

        int direction = 1;
        if (end - start < 0) {
            direction = -1;
        }

        double motorBaseSpeed = positioner.getBaseSpeed();
        double motorHighLimit = positioner.getMaxValue();
        double motorLowLimit = positioner.getMinValue();
        double motorBacklash = positioner.getBacklash();

        boolean respectMotorMinSpeed = false; // if false set min speed to 0
        double motorMinSpeed = 0;
        if (respectMotorMinSpeed) {
            motorMinSpeed = motorBaseSpeed;
        }

        // Check user parameters
        // TODO start and end values must be between the motor high and low value - otherwise fail
        if (start > motorHighLimit || start < motorLowLimit) {
            // Start value is outside motor high and/or low value
            logger.info("Start value is outside motor high and/or low value");
            throw new IllegalArgumentException("Start value is outside motor high and/or low value");
        }
        if (end > motorHighLimit || end < motorLowLimit) {
            // End value is outside motor high and/or low value
            logger.info("End value is outside motor high and/or low value");
            throw new IllegalArgumentException("End value is outside motor high and/or low value");
        }
        // TODO Check minimum step size
        int minimumTicks = 10;
        double minStepSize = motorMinSpeed * (minimumTicks / tps);
        if (stepSize < minStepSize) {
            // Step size is too small
            logger.info("Step size is too small");
            throw new IllegalArgumentException("Step size is too small");
        }
        // TODO Check integration time
        if (motorMinSpeed > 0) {
            double maxIntegrationTime = stepSize / motorMinSpeed;
            if (integrationTime > maxIntegrationTime) {
                logger.info("Integration time is too big");
                // Integration time is too big
                throw new IllegalArgumentException("Integration time is too big");
            }
        }
        double motorMaxSpeed = positioner.getSpeed();
        double minIntegrationTime = Math.min((stepSize / motorMaxSpeed), ((double) minimumTicks / (double) tps));
        if (integrationTime < minIntegrationTime) {
            // Integration time is too small
            logger.info("Integration time is too small [min integration time: " + minIntegrationTime + "]");
            throw new IllegalArgumentException("Integration time is too small [min integration time: " + minIntegrationTime + "]");
        }

        // TODO Calculate and set motor speed, backlash, etc.
        double motorSpeed = stepSize / integrationTime;
        double backlash = (0.5 * motorSpeed * positioner.getAccelerationTime()) + motorBacklash + additionalBacklash;
        double realEnd = end + (backlash * direction);
        double realStart = start - (backlash * direction);

        // Move to start
        logger.info("Move motor to start [" + realStart + "]");
        positioner.move(realStart, timeout);

        // Set motor paramters
        // Backup settings
        logger.info("Backup motor settings");
        double backupSpeed = positioner.getSpeed();
        double backupBacklash = motorBacklash;
        double backupBaseSpeed = motorBaseSpeed;

        if (positioner.getUseExtEncoder()) {
            extEncoderOffset = positioner.getReadback().read() - positioner.read();
        }
        try {
            // Set motor settings
            logger.info("Update motor settings");
//				if(!respectMotorMinSpeed){
//					motortemplate.getBaseSpeed().setValue(0d);
//				}
            // Set base speed as fast as possible but not faster than the original base speed.
            double base = motorBaseSpeed;
            if (motorSpeed < base) {
                base = motorSpeed;
            }
            positioner.setBaseSpeed(base);
            positioner.setSpeed(motorSpeed);
            positioner.setBacklash(0d);
            // Start crlogic logic
            logger.info("Start CRLOGIC");
            template.getStatus().setValue(TemplateCrlogic.Status.INITIALIZE.toString());
            try {
                template.getStatus().waitForValue(TemplateCrlogic.Status.ACTIVE.toString(), startStopTimeout);
            } catch (ChannelException | ExecutionException | TimeoutException e) {
                logger.info("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue());
                if (template.getStatus().getValue().equals(TemplateCrlogic.Status.FAULT.toString())) {
                    logger.info("Error message: " + template.getMessage().getValue());
                }
                // Recover to inactive
                template.getStatus().setValue(TemplateCrlogic.Status.INACTIVE.toString());
                // TODO Improve error handling
                throw new RuntimeException("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue() + " Error message: " + template.getMessage().getValue(), e);

            }

            // Connect ZMQ stream
            connect(ioc);
//				drainHangingSubmessages();
//            listener.onStartOfStream();
            // Move motor(s) to end / wait until motor is stopped
            logger.info("Move motor to end [" + realEnd + "]");
            onBeforeStream(getCurrentPass());
            try {

                // This is the continuous motor move
                Future<Double> future = positioner.moveAsync(realEnd);

                long timeoutTime = System.currentTimeMillis() + timeout;

                // This loop will keep spinning until the motor reached the final position
                while (!future.isDone()) {
                    if (System.currentTimeMillis() > timeoutTime) {
                        throw new TimeoutException("Motion timed out");
                    }

                    receive(); // TODO Problem still blocking

                    if (isAborted()) {
                        logger.info("Scan aborted: stopping positioner");
                        positioner.stop();
                        break;
                    }
                }
                logger.info("Motor reached end position");
            } finally {
                logger.info("End of Scan");
                receive();
                onAfterStream(getCurrentPass());
                closeStream();
                logger.info("Stream closed");
            }

            // Stop crlogic logic
            logger.info("Stop CRLOGIC");
            template.getStatus().setValue(TemplateCrlogic.Status.STOP.toString());
            // Wait until stopped
            logger.info("Wait until stopped");
            try {
                template.getStatus().waitForValue(TemplateCrlogic.Status.INACTIVE.toString(), startStopTimeout);
            } catch (ChannelException | ExecutionException | TimeoutException e) {
                logger.info("Failed to stop CRLOGIC. Logic in status: " + template.getStatus().getValue());
                // TODO Improve error handling
                throw new RuntimeException("Failed to stop CRLOGIC.  Logic in status: " + template.getStatus().getValue(), e);
            }
            logger.info("CRLOGIC is now stopped");
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        } finally {
            logger.info("Restore motor settings");
            positioner.setBaseSpeed(backupBaseSpeed);
            positioner.setSpeed(backupSpeed);
            positioner.setBacklash(backupBacklash);
        }
        executing.set(false);
        synchronized (executing) {
            executing.notifyAll();
        }
    }

    /**
     * Abort execution
     */
    @Override
    protected void doAbort() throws InterruptedException {
        logger.info("Wait execution stop...");
        while (executing.get() == false) {
            synchronized (executing) {
                executing.wait();
            }
        }
    }

    @Override
    protected void prepare() {
        try {
            positioner.assertInitialized();
            // Connect crlogic channels
            template = new TemplateCrlogic();
            logger.info("Connect channels");
            Map<String, String> map = new HashMap<>();
            map.put("PREFIX", prefix);
            Epics.getChannelFactory().createAnnotatedChannels(template, map);

            // TODO build up list of readout resources (based on sensors)
            readoutResources.clear();
            readoutResources.add(positioner.getReadbackKey());
            for (CrlogicSensor s : sensors) {
                s.resetCache();
                readoutResources.add(s.getKey());
            }

            // Workaround - somehow one has to add an empty thing to the value otherwise the c logic 
            // does not pick up the end
            readoutResources.add("");
        } catch (Exception e) {
            throw new RuntimeException("Unable to prepare crloop: " + e.getMessage(), e);
        }
    }

    /**
     * Connect ZMQ stream
     *
     * @param address	ZMQ endpoint address
     */
    private void connect(String address) {
        // Clear interrupted state
        Thread.interrupted();

        logger.info("Connecting with IOC" + address);
        context = ZMQ.context(1);
        socket = context.socket(ZMQ.PULL);
        socket.setRcvHWM(HIGH_WATER_MARK);
        socket.connect("tcp://" + address + ":9999");
    }

    /**
     * Close ZMQ stream
     */
    private void closeStream() {
        logger.info("Closing stream from IOC " + ioc);
        try {
            socket.close();
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
        try {
            context.close();
        } catch (Exception ex) {
            logger.log(Level.INFO, null, ex);
        }
        socket = null;
        context = null;
    }

    /**
     * Drain sub-messages
     *
     * @return	Number of sub-messages drained
     */
    private int drainHangingSubmessages() {
        int count = 0;
        while (socket.hasReceiveMore()) {
            // is there a way to avoid copying data to user space here?
            socket.recv();
            count++;
        }
        if (count > 0) {
            logger.info("Drain hanging submessages");
        }
        return count;
    }

    @Override
    public void cleanup() throws IOException {
        if (template != null) {
            try {
                Epics.getChannelFactory().destroyAnnotatedChannels(template);
            } catch (Exception ex) {
                throw new IOException(ex);
            } finally {
                template = null;
            }
        }
    }
}
