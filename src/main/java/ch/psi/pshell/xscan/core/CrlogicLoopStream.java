package ch.psi.pshell.xscan.core;

import ch.psi.utils.EventBus;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.Metadata;
import ch.psi.jcae.ChannelException;
import ch.psi.jcae.ChannelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * While using Crlogic the IOC system clock rate should/must be set to 1000 (default 60)
 *
 * sysClkRateSet 1000
 *
 */
public class CrlogicLoopStream implements ActionLoop {

    private static final Logger logger = Logger.getLogger(CrlogicLoopStream.class.getName());

    public static final int HIGH_WATER_MARK = 100;
    private Context context;
    private Socket socket;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Flag to indicate whether the data of this loop will be grouped According to this flag the dataGroup flag in
     * EndOfStream will be set.
     */
    private boolean dataGroup = false;

    // Default timeout (in milliseconds) for wait operations
    private long startStopTimeout = 8000;

    private String ioc;

    /**
     * Flag whether the actor of this loop should move in zig zag mode
     */
    private final boolean zigZag;

    private boolean useReadback;
    private boolean useEncoder;

    private List<Action> preActions;
    private List<Action> postActions;
    private List<CrlogicResource> sensors;

    // Prefix for the CRLOGIC channels
    private final String prefix;

    private TemplateCrlogic template;
    private TemplateMotor motortemplate;

    private List<String> readoutResources;
    private Map<Integer, CrlogicDeltaDataFilter> scalerIndices;
    private CrlogicRangeDataFilter crlogicDataFilter;

    private boolean abort = false;

    private final ChannelService cservice;

    private String id;
    private String name; // name of the motor channel
    private String readback; // name of the encoder channel
    private double start;
    private double end;
    private double stepSize;
    private double integrationTime;
    private double additionalBacklash;

    private final EventBus eventbus;
    private List<Metadata> metadata;

    /**
     * Constructor
     *
     * @param cservice	Channel Access context
     * @param prefix	Prefix of the IOC channels
     * @param ioc	Name of the IOC running CRLOGIC
     * @param zigZag	Do zigZag scan
     */
    public CrlogicLoopStream(ChannelService cservice, String prefix, String ioc, boolean zigZag) {
        eventbus = new EventBus();
        this.cservice = cservice;
        this.prefix = prefix;
        this.ioc = ioc;
        this.zigZag = zigZag;

        // Initialize lists used by the loop
        this.preActions = new ArrayList<Action>();
        this.postActions = new ArrayList<Action>();
        this.sensors = new ArrayList<>();
        this.readoutResources = new ArrayList<String>();
        this.scalerIndices = new HashMap<Integer, CrlogicDeltaDataFilter>();

        this.crlogicDataFilter = new CrlogicRangeDataFilter();
    }

    public void setActuator(String id, String name, String readback, double start, double end, double stepSize, double integrationTime, double additionalBacklash) {
        this.id = id;
        this.name = name;
        this.readback = readback;
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
        this.integrationTime = integrationTime;
        this.additionalBacklash = additionalBacklash;
    }

    /**
     * Receive a ZMQ message
     */
    private void receive() {

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

        DataMessage message = new DataMessage(metadata);
        boolean use = true;

        for (int i = 0; i < mainHeader.getElements(); i++) {
            Double raw = byteBuffer.getDouble();
            Double val;

            if (i == 0) {
                if (useEncoder) {
                    val = crlogicDataFilter.calculatePositionMotorUseEncoder(raw);
                } else if (useReadback) {
                    val = crlogicDataFilter.calculatePositionMotorUseReadback(raw);
                } else {
                    val = crlogicDataFilter.calculatePositionMotor(raw);
                }

                // Check whether data is within the configured range - otherwise drop data
                use = crlogicDataFilter.filter(val);
            } else if (scalerIndices.containsKey(i)) {
                CrlogicDeltaDataFilter f = scalerIndices.get(i);
                val = f.delta(raw);
            } else {
                val = raw;
            }

            message.getData().add(val);
        }

        // Drain remaining messages
        int n = drainHangingSubmessages();
        if (n > 1) {
            throw new RuntimeException("More than 1 message drained from stream: " + n);
        }

        if (use) {
            eventbus.post(message);
        }
    }

    @Override
    public void execute() throws InterruptedException {
        try {

            // Set values for the datafilter
            crlogicDataFilter.setStart(start);
            crlogicDataFilter.setEnd(end);

            // Reset data filter
            for (Integer k : scalerIndices.keySet()) {
                scalerIndices.get(k).reset();
            }

            // Set abort state to false
            abort = false;

            Long timeout = 600000l; // 10 minutes move timeout

            // Check if logic is inactive, otherwise return early
            if (!template.getStatus().getValue().equals(TemplateCrlogic.Status.INACTIVE.toString())) {
                logger.info("CRLOGIC is not inactive!");
                // TODO Decide what to do in this situation

                if (template.getStatus().getValue().equals(TemplateCrlogic.Status.FAULT.toString())) {
                    // If in fault show message and recover
                    logger.info("CRLOGIC in FAULT state");
                    logger.info("Error message: " + template.getMessage().getValue());
                    logger.info("Recover logic and set it to INACTIVE");
                    template.setStatus(TemplateCrlogic.Status.INACTIVE);
                } else if (template.getStatus().getValue().equals(TemplateCrlogic.Status.ACTIVE.toString())) {
                    template.setStatus(TemplateCrlogic.Status.STOP);
                    template.waitStatus(TemplateCrlogic.Status.INACTIVE, startStopTimeout);
                } else {
                    throw new RuntimeException("CRLOGIC is not inactive");
                }
            }

            logger.info("Set parameters");

            /*
java.lang.RuntimeException: java.util.concurrent.ExecutionException: java.lang.ArrayIndexOutOfBoundsException: 1001 >= 1001
	at ch.psi.pshell.xscan.core.loops.cr.ParallelCrlogic.execute(ParallelCrlogic.java:165)
	at ch.psi.pshell.xscan.core.loops.ActorSensorLoop.execute(ActorSensorLoop.java:314)
	at ch.psi.pshell.xscan.aq.Acquisition.execute(Acquisition.java:365)
	at ch.psi.pshell.xscan.ProcessorFDA.doExecution(ProcessorFDA.java:623)
	at ch.psi.pshell.xscan.ProcessorFDA$FDATask.doInBackground(ProcessorFDA.java:440)
	at ch.psi.pshell.xscan.ProcessorFDA$FDATask.doInBackground(ProcessorFDA.java:420)
	at java.desktop/javax.swing.SwingWorker$1.call(SwingWorker.java:304)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.desktop/javax.swing.SwingWorker.run(SwingWorker.java:343)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
	at java.base/java.lang.Thread.run(Thread.java:835)
Caused by: java.util.concurrent.ExecutionException: java.lang.ArrayIndexOutOfBoundsException: 1001 >= 1001
	at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122)
	at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191)
	at ch.psi.pshell.xscan.core.loops.cr.ParallelCrlogic.execute(ParallelCrlogic.java:162)
	... 11 more
Caused by: java.lang.ArrayIndexOutOfBoundsException: 1001 >= 1001
	at java.base/java.util.Vector.elementAt(Vector.java:496)
	at java.desktop/javax.swing.table.DefaultTableModel.justifyRows(DefaultTableModel.java:281)
	at java.desktop/javax.swing.table.DefaultTableModel.insertRow(DefaultTableModel.java:382)
	at java.desktop/javax.swing.table.DefaultTableModel.addRow(DefaultTableModel.java:357)
	at java.desktop/javax.swing.table.DefaultTableModel.addRow(DefaultTableModel.java:368)
	at ch.psi.pshell.swing.LoggerPanel.addRow(LoggerPanel.java:163)
	at ch.psi.pshell.swing.LoggerPanel$3.publish(LoggerPanel.java:105)
	at java.logging/java.util.logging.Logger.log(Logger.java:979)
	at java.logging/java.util.logging.Logger.doLog(Logger.java:1006)
	at java.logging/java.util.logging.Logger.log(Logger.java:1029)
	at java.logging/java.util.logging.Logger.info(Logger.java:1802)
	at ch.psi.pshell.xscan.core.loops.cr.CrlogicLoopStream.execute(CrlogicLoopStream.java:264)
	at ch.psi.pshell.xscan.core.loops.cr.ParallelCrlogic$1.call(ParallelCrlogic.java:127)
	at ch.psi.pshell.xscan.core.loops.cr.ParallelCrlogic$1.call(ParallelCrlogic.java:115)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	... 3 more   */
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

            double motorBaseSpeed = motortemplate.getBaseSpeed().getValue();
            double motorHighLimit = motortemplate.getHighLimit().getValue();
            double motorLowLimit = motortemplate.getLowLimit().getValue();
            double motorBacklash = motortemplate.getBacklashDistance().getValue();

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
            double motorMaxSpeed = motortemplate.getVelocity().getValue();
            double minIntegrationTime = Math.min((stepSize / motorMaxSpeed), ((double) minimumTicks / (double) tps));
            if (integrationTime < minIntegrationTime) {
                // Integration time is too small
                logger.info("Integration time is too small [min integration time: " + minIntegrationTime + "]");
                throw new IllegalArgumentException("Integration time is too small [min integration time: " + minIntegrationTime + "]");
            }

            // TODO Calculate and set motor speed, backlash, etc.
            double motorSpeed = stepSize / integrationTime;
            double backlash = (0.5 * motorSpeed * motortemplate.getAccelerationTime().getValue()) + motorBacklash + additionalBacklash;
            double realEnd = end + (backlash * direction);
            double realStart = start - (backlash * direction);

            // Move to start
            logger.info("Move motor to start [" + realStart + "]");
            motortemplate.getSetValue().setValueAsync(realStart).get(timeout, TimeUnit.MILLISECONDS); // Will block until move is done

            // Set motor paramters
            // Backup settings
            logger.info("Backup motor settings");
            double backupSpeed = motortemplate.getVelocity().getValue();
            double backupBacklash = motorBacklash;
            double backupMinSpeed = motorBaseSpeed;

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
                motortemplate.getBaseSpeed().setValue(base);
                motortemplate.getVelocity().setValue(motorSpeed);
                motortemplate.getBacklashDistance().setValue(0d);

                // Execute pre actions
                for (Action action : preActions) {
                    action.execute();
                }

// Start crlogic logic
                logger.info("Start CRLOGIC");
                template.setStatus(TemplateCrlogic.Status.INITIALIZE);
                try {
                    template.waitStatus(TemplateCrlogic.Status.ACTIVE, startStopTimeout);
                } catch (ChannelException | ExecutionException | TimeoutException e) {
                    logger.info("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue());
                    if (template.getStatus().getValue().equals(TemplateCrlogic.Status.FAULT.toString())) {
                        logger.info("Error message: " + template.getMessage().getValue());
                    }
                    // Recover to inactive
                    template.setStatus(TemplateCrlogic.Status.INACTIVE);
                    // TODO Improve error handling
                    throw new RuntimeException("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue() + " Error message: " + template.getMessage().getValue(), e);

                }

                // Connect ZMQ stream
                connect(ioc);
//				drainHangingSubmessages();

                // Move motor(s) to end / wait until motor is stopped
                logger.info("Move motor to end [" + realEnd + "]");
                try {

                    // This is the continuous motor move
                    Future<Double> future = motortemplate.getSetValue().setValueAsync(realEnd);

                    long timeoutTime = System.currentTimeMillis() + timeout;

                    // This loop will keep spinning until the motor reached the final position
                    while (!future.isDone()) {
                        if (System.currentTimeMillis() > timeoutTime) {
                            throw new TimeoutException("Motion timed out");
                        }

                        receive(); // TODO Problem still blocking

                        if (abort) {
                            // Abort motor move 
                            motortemplate.getCommand().setValue(TemplateMotor.Commands.Stop.ordinal());
                            motortemplate.getCommand().setValue(TemplateMotor.Commands.Go.ordinal());
                            break;
                        }

                    }
                } finally {
                    receive();

                    // Send end of stream message
                    logger.info("Sending - End of Line - Data Group: " + dataGroup);
                    eventbus.post(new EndOfStreamMessage(dataGroup));

                    // Close ZMQ stream
                    close();
                }
                logger.info("Motor reached end position");

                // Stop crlogic logic
                logger.info("Stop CRLOGIC");
                template.setStatus(TemplateCrlogic.Status.STOP);
                // Wait until stopped
                logger.info("Wait until stopped");
                try {
                    template.waitStatus(TemplateCrlogic.Status.INACTIVE, startStopTimeout);
                } catch (ChannelException | ExecutionException | TimeoutException e) {
                    logger.info("Failed to stop CRLOGIC. Logic in status: " + template.getStatus().getValue());
                    // TODO Improve error handling
                    throw new RuntimeException("Failed to stop CRLOGIC.  Logic in status: " + template.getStatus().getValue(), e);
                }
                logger.info("CRLOGIC is now stopped");

                // Execute post actions
                for (Action action : postActions) {
                    action.execute();
                }

            } finally {
                logger.info("Restore motor settings");
                motortemplate.getBaseSpeed().setValue(backupMinSpeed);
                motortemplate.getVelocity().setValue(backupSpeed);
                motortemplate.getBacklashDistance().setValue(backupBacklash);
            }

            if (zigZag) {
                // reverse start/end
                double aend = end;
                end = start;
                start = aend;
            }

        } catch (ChannelException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Unable to execute crloop", e);
        }
    }

    /**
     * Abort execution
     */
    @Override
    public void abort() {
        abort = true;
    }

    /**
     * Prepare this loop for execution
     */
    @Override
    public void prepare() {

        metadata = new ArrayList<>();

        // Build up metadata
        metadata.add(new Metadata(this.id));
        for (CrlogicResource s : sensors) {
            metadata.add(new Metadata(s.getId()));
        }

        try {
            // Connect crlogic channels
            template = new TemplateCrlogic();
            logger.info("Connect channels");
            Map<String, String> map = new HashMap<>();
            map.put("PREFIX", prefix);
            cservice.createAnnotatedChannels(template, map);

            // Connect motor channels
            motortemplate = new TemplateMotor();
            map = new HashMap<>();
            map.put("PREFIX", this.name);
            cservice.createAnnotatedChannels(motortemplate, map);

            useReadback = motortemplate.getUseReadback().getValue();
            useEncoder = motortemplate.getUseEncoder().getValue();

            logger.info("Motor type: " + TemplateMotor.Type.values()[motortemplate.getType().getValue()]);
            logger.info("Motor use readback: " + useReadback);
            logger.info("Motor use encoder: " + useEncoder);

            // TODO build up list of readout resources (based on sensors)
            readoutResources.clear();
            // first sensor is the actuator

            // Determine mode of motor
            if ((!useReadback) && (!useEncoder)) {
                // Open loop
                if (this.readback != null) {
                    throw new IllegalArgumentException("Readback not supported if motor is configured in open loop");
                } else {
                    readoutResources.add(this.name);
                }

            } else if (useReadback && (!useEncoder)) {
                String readback;
                // use readback link
                if (this.readback != null) {
                    // Use specified readback
                    readback = this.readback;
                } else {
                    // Set resouce to readback link
                    readback = (motortemplate.getReadbackLink().getValue());
                    readback = readback.replaceAll(" +.*", ""); // remove NPP etc at the end
                }

                readoutResources.add(readback);

                // Fill readback encoder settings
                // Connect to encoder
                TemplateEncoder encodertemplate = new TemplateEncoder();
                map = new HashMap<>();
                map.put("PREFIX", readback);
                cservice.createAnnotatedChannels(encodertemplate, map);

                // Read encoder settings
                if (encodertemplate.getDirection().getValue() == TemplateEncoder.Direction.Positive.ordinal()) {
                    crlogicDataFilter.setEncoderDirection(1);
                } else {
                    crlogicDataFilter.setEncoderDirection(-1);
                }
                crlogicDataFilter.setEncoderOffset(encodertemplate.getOffset().getValue());
                crlogicDataFilter.setEncoderResolution(encodertemplate.getResolution().getValue());

                // Disconnect from encoder
                cservice.destroyAnnotatedChannels(encodertemplate);

            } else if (useEncoder && (!useReadback)) {
                // use readback link
                if (this.readback != null) {
                    throw new IllegalArgumentException("Readback not supported if motor is configured to use encoder");
                } else {
                    // Set resouce to readback link
                    readoutResources.add(this.name + "_ENC");
                }
            } else {
                throw new IllegalArgumentException("Motor configuration not supportet: use readback - " + useReadback + " use encoder - " + useEncoder);
            }

            // Fill Motor specific settings
            if (motortemplate.getDirection().getValue() == TemplateMotor.Direction.Positive.ordinal()) {
                crlogicDataFilter.setMotorDirection(1);
            } else {
                crlogicDataFilter.setMotorDirection(-1);
            }
            crlogicDataFilter.setMotorEncoderResolution(motortemplate.getEncoderResolution().getValue());
            crlogicDataFilter.setMotorOffset(motortemplate.getOffset().getValue());
            crlogicDataFilter.setMotorReadbackResolution(motortemplate.getReadbackResolution().getValue());
            crlogicDataFilter.setMotorResolution(motortemplate.getMotorResolution().getValue());

            // Clear all indices
            scalerIndices.clear();

            int c = 1; // We start at 1 because the actuator right now is an implicit sensor 
            for (CrlogicResource s : sensors) {
                readoutResources.add(s.getKey());
                if (s.isDelta()) {
                    scalerIndices.put(c, new CrlogicDeltaDataFilter());
                }
                c++;
            }

            // Workaround - somehow one has to add an empty thing to the value otherwise the c logic 
            // does not pick up the end
            readoutResources.add("");
        } catch (Exception e) {
            throw new RuntimeException("Unable to prepare crloop: ", e);
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
    private void close() {
        logger.info("Closing stream from IOC " + ioc);

        socket.close();
        context.close();
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

    /**
     * Cleanup loop
     */
    @Override
    public void cleanup() {
        logger.info("Cleanup");
        try {
            cservice.destroyAnnotatedChannels(template);
            cservice.destroyAnnotatedChannels(motortemplate);
            template = null;
            motortemplate = null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to destroy Channel Access channels", e);
        }
    }

    @Override
    public List<Action> getPreActions() {
        return preActions;
    }

    @Override
    public List<Action> getPostActions() {
        return postActions;
    }

    public List<CrlogicResource> getSensors() {
        return sensors;
    }

    @Override
    public boolean isDataGroup() {
        return dataGroup;
    }

    @Override
    public void setDataGroup(boolean dataGroup) {
        this.dataGroup = dataGroup;
    }

    public EventBus getEventBus() {
        return eventbus;
    }
}
