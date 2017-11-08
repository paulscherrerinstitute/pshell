package ch.psi.pshell.crlogic;

import ch.psi.jcae.ChannelException;
import ch.psi.pshell.device.Device;
import ch.psi.pshell.device.Movable;
import ch.psi.pshell.device.ReadbackDevice;
import ch.psi.pshell.device.ReadonlyAsyncRegisterBase;
import ch.psi.pshell.device.Register;
import ch.psi.pshell.device.RegisterBase;
import ch.psi.pshell.device.Resolved;
import ch.psi.pshell.device.Speedable;
import ch.psi.pshell.epics.ChannelDouble;
import ch.psi.pshell.epics.Epics;
import ch.psi.utils.Reflection.Hidden;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class CrlogicPositioner extends RegisterBase<Double> implements ReadbackDevice<Double>, Movable<Double>, Speedable, Resolved{

    final String key;
    final String positionerReadback;
    String readbackKey;
    final ReadbackRegister readbackRegister;
    boolean useReadback;
    boolean useEncoder;
    ChannelDouble extEncoder;

    private TemplateMotor motortemplate;

    private double motorResolution = 1;
    private int motorDirection = 1;
    private double motorOffset = 0;
    private double motorReadbackResolution = 1;
    private double motorEncoderResolution = 1;

    private double encoderOffset = 0;
    private double encoderResolution = 1;
    private int encoderDirection = 1;

    class ReadbackRegister extends ReadonlyAsyncRegisterBase<Double> {

        ReadbackRegister() {
            super(CrlogicPositioner.this.getName() + " readback");
            setMonitored(true);
        }

        void setRawValue(Double value) {
            if (getUseEncoder()) {
                setCache(calculatePositionMotorUseEncoder(value));
            } else if (getUseReadback()) {
                setCache(calculatePositionMotorUseReadback(value));
            } else if (getUseExtEncoder()) {
                setCache(calculatePositionMotorUseExtEncoder(value));
            } else {
                setCache(calculatePositionMotor(value));
            }
        }

        @Override
        protected Double doRead() throws IOException, InterruptedException {
            if (motortemplate == null) {
                return null;
            }
            try {
                if (getUseExtEncoder()) {
                    return extEncoder.read();
                } else {
                    return motortemplate.getReadbackValue().getValue();
                }
            } catch (java.util.concurrent.TimeoutException | ChannelException | ExecutionException ex) {
                throw new IOException(ex);
            }
        }
    }

    public CrlogicPositioner(String name, String key) {
        this(name, key, null);
    }

    public CrlogicPositioner(String name, String key, String positionerReadback) {
        super(name);
        this.key = key;
        this.positionerReadback = positionerReadback;
        readbackRegister = new ReadbackRegister();
        setChildren(new Device[]{readbackRegister});
    }

    @Override
    protected void doInitialize() throws IOException, InterruptedException {
        super.doInitialize();
        destroyTemplate();
        if (extEncoder != null) {
            extEncoder.close();
            extEncoder = null;
        }

        // Connect motor channels
        motortemplate = new TemplateMotor();
        Map<String, String> map = new HashMap<>();
        map.put("PREFIX", getKey());
        try {
            Epics.getChannelFactory().createAnnotatedChannels(motortemplate, map);
            //If 
            useReadback = motortemplate.getUseReadback().getValue();
            useEncoder = motortemplate.getUseEncoder().getValue();

            getLogger().info("Motor type: " + TemplateMotor.Type.values()[motortemplate.getType().getValue()]);
            getLogger().info("Motor use readback: " + getUseReadback());
            getLogger().info("Motor use encoder: " + getUseEncoder());
            getLogger().info("Motor use ext encoder: " + getUseExtEncoder());

            // Determine mode of motor           
            if (getUseReadback() && (!getUseEncoder())) {
                if (getPositionerReadback() != null) {
                    // Use specified readback
                    readbackKey = getPositionerReadback();
                } else {
                    // Set resouce to readback link
                    readbackKey = (motortemplate.getReadbackLink().getValue());
                    readbackKey = readbackKey.replaceAll(" +.*", ""); // remove NPP etc at the end
                }
                loadEncoderSettings();
            } else if (getUseEncoder() && (!getUseReadback())) {
                // use readback link
                if (getPositionerReadback() != null) {
                    throw new IllegalArgumentException("Readback not supported if motor is configured to use encoder");
                } else {
                    // Set resouce to readback link
                    readbackKey = getKey() + "_ENC";
                }
            } else if ((!getUseReadback()) && (!getUseEncoder())) {
                // Open loop
                if (getUseExtEncoder()) {
                    extEncoder = new ChannelDouble(getName() + " ext readback", getPositionerReadback());
                    extEncoder.initialize();
                    //Readback refers to external encoder
                    readbackKey = getPositionerReadback();
                    loadEncoderSettings();
                } else {
                    readbackKey = getKey();
                }
            } else {
                throw new IllegalArgumentException("Motor configuration not supportet: use readback - " + getUseReadback() + " use encoder - " + getUseEncoder());
            }

            // Fill Motor specific settings
            if (motortemplate.getDirection().getValue() == TemplateMotor.Direction.Positive.ordinal()) {
                motorDirection = 1;
            } else {
                motorDirection = -1;
            }
            motorEncoderResolution = motortemplate.getEncoderResolution().getValue();
            motorOffset = motortemplate.getOffset().getValue();
            motorReadbackResolution = motortemplate.getReadbackResolution().getValue();
            motorResolution = motortemplate.getMotorResolution().getValue();
        } catch (IOException | InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    void loadEncoderSettings() throws Exception {
        // Connect to encoder
        TemplateEncoder encodertemplate = new TemplateEncoder();
        Map<String, String> map = new HashMap<>();
        map.put("PREFIX", getPositionerReadback());
        Epics.getChannelFactory().createAnnotatedChannels(encodertemplate, map);

        // Read encoder settings
        if (encodertemplate.getDirection().getValue() == TemplateEncoder.Direction.Positive.ordinal()) {
            encoderDirection = 1;
        } else {
            encoderDirection = -1;
        }
        encoderOffset = encodertemplate.getOffset().getValue();
        encoderResolution = encodertemplate.getResolution().getValue();

        // Disconnect from encoder
        Epics.getChannelFactory().destroyAnnotatedChannels(encodertemplate);
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        super.doUpdate();
        getReadback().read();
    }

    @Override
    protected Double doRead() throws IOException, InterruptedException {
        try {
            return motortemplate.getSetValue().getValue();
        } catch (java.util.concurrent.TimeoutException | ChannelException | ExecutionException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected void doWrite(Double value) throws IOException, InterruptedException {
        moveAsync(value);
    }

    @Override
    public Register<Double> getReadback() {
        return readbackRegister;
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        try {
            motortemplate.getCommand().setValueAsync(TemplateMotor.Commands.Stop.ordinal());
            motortemplate.getCommand().setValueAsync(TemplateMotor.Commands.Go.ordinal());
        } catch (ChannelException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void move(Double destination, int timeout) throws IOException, InterruptedException {
        try {
            setCache(destination);
            Future<Double> future = motortemplate.getSetValue().setValueAsync(destination);
            if (timeout > 0) {
                future.get(timeout, TimeUnit.MILLISECONDS);
            } else {
                future.get();
            }
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Double getPosition() throws IOException, InterruptedException {
        return getReadback().take();
    }

    @Override
    public boolean isInPosition(Double pos) throws IOException, InterruptedException {
        return Math.abs(getPosition() - read()) <= Math.abs(getResolution());
    }

    public double getResolution() {
        if (getUseEncoder()) {
            return motorEncoderResolution * motorReadbackResolution;
        } else if (getUseReadback()) {
            return encoderResolution * motorReadbackResolution;
        } else {
            return calculatePositionMotor(motorResolution * motorReadbackResolution);
        }
    }

    @Override
    public void waitInPosition(Double pos, int timeout) throws IOException, InterruptedException {
        getReadback().waitValueInRange(pos, getResolution(), timeout);
    }

    @Hidden
    @Override
    public void assertInPosition(Double pos) throws IOException, InterruptedException {
        if (!isInPosition(pos)) {
            throw new DeviceException("Not in position: " + pos);
        }
    }

    public String getKey() {
        return key;
    }

    public String getPositionerReadback() {
        return positionerReadback;
    }

    public String getReadbackKey() {
        return readbackKey;
    }

    /**
     * Calculate real position
     *
     * @param raw
     * @return
     */
    double calculatePositionMotor(double raw) {
        return (((raw * motorResolution * motorReadbackResolution) / motorDirection) + motorOffset);
    }

    /**
     * Calculate real motor position using the readback link
     *
     * @param raw
     * @return
     */
    double calculatePositionMotorUseReadback(double raw) {
        return ((((raw - encoderOffset) * encoderResolution * encoderDirection * motorReadbackResolution) / motorDirection) + motorOffset);
    }

    /**
     * Calculate real motor position using encoder
     *
     * @param raw
     * @return
     */
    double calculatePositionMotorUseEncoder(double raw) {
        return (raw * motorEncoderResolution * motorReadbackResolution / motorDirection + motorOffset);
    }

    double calculatePositionMotorUseExtEncoder(double raw) {
        return ((((raw - encoderOffset) * encoderResolution * encoderDirection * motorReadbackResolution) / motorDirection));
    }

    @Override
    protected void doClose() throws IOException {
        super.doClose();
        destroyTemplate();
        if (extEncoder != null) {
            extEncoder.close();
            extEncoder = null;
        }
    }

    void destroyTemplate() throws IOException {
        if (motortemplate != null) {
            try {
                Epics.getChannelFactory().destroyAnnotatedChannels(motortemplate);
            } catch (Exception ex) {
                throw new IOException(ex);
            } finally {
                motortemplate = null;
            }
        }
    }

    boolean getUseReadback() {
        return useReadback;
    }

    boolean getUseEncoder() {
        return useEncoder;
    }

    boolean getUseExtEncoder() {
        return !useReadback && !useEncoder && (getPositionerReadback() != null);
    }

    public Double getBaseSpeed() throws IOException, InterruptedException {
        try {
            return motortemplate.getBaseSpeed().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Double getSpeed() throws IOException, InterruptedException {
        try {
            return motortemplate.getVelocity().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public void setSpeed(Double speed) throws IOException, InterruptedException {
        try {
            motortemplate.getVelocity().setValueAsync(speed);
        } catch (ChannelException ex) {
            throw new IOException(ex);
        }
    }

    public void setBaseSpeed(Double value) throws IOException, InterruptedException {
        try {
            motortemplate.getBaseSpeed().setValueAsync(value);
        } catch (ChannelException ex) {
            throw new IOException(ex);
        }
    }

    public double getMinValue() throws IOException, InterruptedException {
        try {
            return motortemplate.getLowLimit().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public double getMaxValue() throws IOException, InterruptedException {
        try {
            return motortemplate.getHighLimit().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public double getAccelerationTime() throws IOException, InterruptedException {
        try {
            return motortemplate.getAccelerationTime().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public double getBacklash() throws IOException, InterruptedException {
        try {
            return motortemplate.getBacklashDistance().getValue();
        } catch (ChannelException | ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(ex);
        }
    }

    public void setBacklash(Double value) throws IOException, InterruptedException {
        try {
            motortemplate.getBacklashDistance().setValueAsync(value);
        } catch (ChannelException ex) {
            throw new IOException(ex);
        }
    }

}
