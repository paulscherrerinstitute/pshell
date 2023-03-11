package ch.psi.pshell.xscan.core;

import ch.psi.utils.EventBus;
import ch.psi.pshell.xscan.DataMessage;
import ch.psi.pshell.xscan.EndOfStreamMessage;
import ch.psi.pshell.xscan.Metadata;
import ch.psi.jcae.ChannelService;
import ch.psi.pshell.crlogic.TemplateCrlogic;
import ch.psi.pshell.crlogic.TemplateEncoder;
import ch.psi.pshell.crlogic.TemplateMotor;
import ch.psi.pshell.device.DummyMotor;
import static ch.psi.pshell.device.Record.UNDEFINED_PRECISION;
import ch.psi.pshell.epics.ChannelDoubleArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
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
    
    private ChannelDoubleArray dataChannel; 

    /**
     * Flag to indicate whether the data of this loop will be grouped According to this flag the dataGroup flag in
     * EndOfStream will be set.
     */
    private boolean dataGroup = false;

    // Default timeout (in milliseconds) for wait operations
    private long startStopTimeout = 8000;

    private String ioc;

    final String channel;
        
    /**
     * Flag whether the actor of this loop should move in zig zag mode
     */
    private final boolean zigZag;
    private final boolean simulation;
    private final boolean abortable;
    

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
    
    public CrlogicLoopStream(ChannelService cservice,  boolean zigZag, String prefix, String ioc,  String channel, boolean abortable, boolean simulation) {
        eventbus = new EventBus();
        this.cservice = cservice;
        this.prefix = prefix;
        this.ioc = ioc;
        this.channel=channel;
        this.zigZag = zigZag;
        this.simulation = simulation;
        this.abortable = abortable;
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
        if ((readback!=null) && (readback.isBlank())){
            readback=null;
        }
        this.readback = readback;
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
        this.integrationTime = integrationTime;
        this.additionalBacklash = additionalBacklash;
    }
    
    public boolean isSimulated(){
        return simulation;
    }


    public boolean isAbortable(){
        return abortable;
    }   

    /**
     * Receive a ZMQ message
     */
    private void receiveZmq() {

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
                if (isExtReadback()){
                    val=raw;
                } else {
                    if (useEncoder) {
                        val = crlogicDataFilter.calculatePositionMotorUseEncoder(raw);
                    } else if (useReadback) {
                        val = crlogicDataFilter.calculatePositionMotorUseReadback(raw);
                    } else {
                        val = crlogicDataFilter.calculatePositionMotor(raw);
                    }

                    // Check whether data is within the configured range - otherwise drop data
                    use = crlogicDataFilter.filter(val);
                }
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
    
    /**
     * Receive a ZMQ message
     */
    private void receiveChannel() throws IOException, InterruptedException {
        //if (!dataChannel.waitCacheChange(10)){
        if (dataChannel.waitBuffer(10)){
            DataMessage message = new DataMessage(metadata);
            boolean use = true;
            double pos = Double.NaN;

            //double[] data = dataChannel.take();
            double[] data = dataChannel.popBuffer().getValue();
            for (int i = 0; i < data.length; i++) {
                double raw = data[i];
                Double val;
                if (i == 0) {
                    if (isExtReadback()){
                        val=raw;
                    } else {
                        if (useEncoder) {
                            val = crlogicDataFilter.calculatePositionMotorUseEncoder(raw);
                        } else if (useReadback) {
                            val = crlogicDataFilter.calculatePositionMotorUseReadback(raw);
                        } else {
                            val = crlogicDataFilter.calculatePositionMotor(raw);
                        }

                        // Check whether data is within the configured range - otherwise drop data
                        use = crlogicDataFilter.filter(val);
                    }
                } else if (i<=sensors.size()){
                    if (scalerIndices.containsKey(i)) {
                        CrlogicDeltaDataFilter f = scalerIndices.get(i);
                        val = f.delta(raw);
                    } else {
                        val = raw;
                }
                } else {
                    break;
                }
                message.getData().add(val);
            }

            if (use) {
                eventbus.post(message); 
            }
        }
    }


    private void receiveSimulation(DummyMotor motor) throws IOException, InterruptedException {

        DataMessage message = new DataMessage(metadata);
        double pos = Double.NaN;

        Double val = motor.getPosition();
        message.getData().add(val);
        
        message.getData().add((double)System.currentTimeMillis());
        
        //double[] data = dataChannel.take();
        for (int i = 0; i < 15; i++) {
            Double raw = i + Math.random();
            if (i<=sensors.size()){
                if (scalerIndices.containsKey(i)) {
                    CrlogicDeltaDataFilter f = scalerIndices.get(i);
                    val = f.delta(raw);
                } else {
                    val = raw;
                }
            } else {
                break;
            }
            message.getData().add(val);
        }

        eventbus.post(message);
        
    }

    
    private void simulate() throws IOException, TimeoutException, InterruptedException{
        logger.warning("CRLOGIC is simulated");
        abort = false;
        int timeout = 600000; // 10 minutes move timeout
        DummyMotor motor = new DummyMotor("crlogic simulation");
        motor.initialize();
        motor.getConfig().minValue=Math.min(start,end);
        motor.getConfig().maxValue=Math.max(start,end);
        double range = motor.getConfig().maxValue-motor.getConfig().minValue;
        motor.getConfig().maxSpeed=range;
        motor.getConfig().defaultSpeed=range/10.0;
        
        // Move to start
        logger.info("Move motor to start [" + start + "]");
        motor.setSpeed(motor.getMaxSpeed());
        motor.move(start, timeout);

        // Execute pre actions
        for (Action action : preActions) {
            action.execute();
        }

        // Move motor(s) to end / wait until motor is stopped
        logger.info("Move motor to end [" + end + "]");
        try {
            motor.setSpeed(motor.getDefaultSpeed());
            Future<Double> future = motor.moveAsync(end);
            long timeoutTime = System.currentTimeMillis() + timeout;

            // This loop will keep spinning until the motor reached the final position
            while (!future.isDone()) {
                if (System.currentTimeMillis() > timeoutTime) {
                    throw new TimeoutException("Motion timed out");
                }
                receiveSimulation(motor);
                if (abort && abortable) {
                    logger.info("CRLOGIC scan aborted");
                    break;
                }
            }
            if (future.isDone()) {
                logger.info("Motor reached end position");
            } else {
                logger.info("Motor didn't reached end position");
            }            
        } finally {
            // Send end of stream message
            logger.info("Sending - End of Line - Data Group: " + dataGroup);
            eventbus.post(new EndOfStreamMessage(dataGroup));
            // Close ZMQ stream
            close();
        }        

        // Stop crlogic logic
        logger.info("Wait until stopped");
        try {
            motor.waitReady((int)startStopTimeout);
        } catch (InterruptedException e) {
            throw e; 
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop CRLOGIC: " + e.getMessage(), e);
        }
        logger.info("CRLOGIC is now stopped");

        // Execute post actions
        for (Action action : postActions) {
            action.execute();
        }

        if (zigZag) {
            // reverse start/end
            double aend = end;
            end = start;
            start = aend;
        }
        
    }

    @Override
    public void execute() throws InterruptedException {
        if (!useIoc() && !useChannel() && !simulation){
            throw new IllegalArgumentException("Either IOC or channel name must be configured");
        }
        boolean usingIoc = useIoc();
        
        try {
            if (simulation){
                simulate();
                return;
            }

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
            if ((motorHighLimit!=0) || (motorLowLimit!=0)){ //low==high==0 => Mo limits
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
                } catch (InterruptedException e) {
                    throw e; 
                } catch (Exception e) {
                    logger.info("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue());
                    if (template.getStatus().getValue().equals(TemplateCrlogic.Status.FAULT.toString())) {
                        logger.info("Error message: " + template.getMessage().getValue());
                    }
                    // Recover to inactive
                    template.setStatus(TemplateCrlogic.Status.INACTIVE);
                    // TODO Improve error handling
                    throw new RuntimeException("Failed to start CRLOGIC. Logic in status: " + template.getStatus().getValue() + " Error message: " + template.getMessage().getValue(), e);

                }

                // Connect to source
                connect();
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

                        if (usingIoc){
                            receiveZmq(); // TODO Problem still blocking
                        } else if (channel != null){
                            receiveChannel();
                        }

                        if (abort && abortable) {
                            logger.info("CRLOGIC scan aborted");
                            // Abort motor move 
                            motortemplate.getCommand().setValue(TemplateMotor.Commands.Stop.ordinal());
                            motortemplate.getCommand().setValue(TemplateMotor.Commands.Go.ordinal());
                            break;
                        }

                    }
                    if (future.isDone()) {
                        logger.info("Motor reached end position");
                    } else {
                        logger.info("Motor didn't reached end position");
                    }
                } finally {
                    if (usingIoc){
                        receiveZmq();
                    }

                    // Send end of stream message
                    logger.info("Sending - End of Line - Data Group: " + dataGroup);
                    eventbus.post(new EndOfStreamMessage(dataGroup));

                    // Close ZMQ stream
                    close();
                }

                // Stop crlogic logic
                logger.info("Stop CRLOGIC");
                template.setStatus(TemplateCrlogic.Status.STOP);
                // Wait until stopped
                logger.info("Wait until stopped");
                try {
                    template.waitStatus(TemplateCrlogic.Status.INACTIVE, startStopTimeout);
                } catch (InterruptedException e) {
                    throw e; 
                } catch (Exception e) {
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

        } catch (InterruptedException e) {
            throw e; 
        } catch (Exception e) {
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
    
    boolean isExtReadback(){
        return  (!useReadback) && (!useEncoder) && (this.readback != null)  && (!this.readback.isBlank());
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

        if (simulation){
            return;
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
                if (this.readback != null) { //isExtReadback = true
                    //throw new IllegalArgumentException("Readback not supported if motor is configured in open loop");
                    readoutResources.add(this.readback);
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
                try{
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
                } catch (Exception ex){
                    logger.warning("Error reading encoder fields: " + ex.getMessage());
                } finally{
                    try{
                    // Disconnect from encoder
                    cservice.destroyAnnotatedChannels(encodertemplate);
                   } catch (Exception ex){
                   } 
                }
            } else if (useEncoder && (!useReadback)) {
                // use readback link
                if (this.readback != null) {
                    //throw new IllegalArgumentException("Readback not supported if motor is configured to use encoder");
                    readoutResources.add(this.readback);
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
     * Connect to source
     *
     * @param address	ZMQ endpoint address
     */
    private void connect() {
        // Clear interrupted state
        Thread.interrupted();

        if (simulation){
            return;
        }
        
        boolean useIoc = (ioc!=null) && !ioc.isBlank();
        if (useIoc()){
            logger.info("Connecting with IOC" + ioc);
            context = ZMQ.context(1);
            socket = context.socket(ZMQ.PULL);
            socket.setRcvHWM(HIGH_WATER_MARK);
            socket.connect("tcp://" + ioc + ":9999");
        } else if (channel  != null){
            logger.info("Connecting to channel: " + channel);
            int size = sensors.size() + 1;
            dataChannel = new ChannelDoubleArray("CrlogicChannel",channel, UNDEFINED_PRECISION, size);
            dataChannel.setMonitored(true);
            dataChannel.setBufferCapacity(100);
            try{
                dataChannel.initialize();
            } catch (Exception e) {
                throw new RuntimeException("Unable to connect to Crlogic source channel: " + channel, e);
            }
        }
    }

    /**
     * Close source
     */
    private void close() {
        if (simulation){
            return;
        }
        
        boolean useIoc = (ioc!=null) && !ioc.isBlank();
        if (useIoc()){
            logger.info("Closing stream from IOC " + ioc);
            try {
                socket.close();
                context.close();
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
            socket = null;
            context = null;
        } else if (channel!=null){
            logger.info("Closing stream from channel " + channel);
            try {
                dataChannel.close();
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        }
        
    }
    
    public boolean useIoc(){
        return (ioc!=null) && !ioc.isBlank();
    }
    
    public boolean useChannel(){
        return !useIoc() && (channel!=null) && !channel.isBlank();
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
        if (simulation){
            return;
        }        
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
