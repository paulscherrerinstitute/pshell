package ch.psi.pshell.device;

import ch.psi.utils.Convert;
import java.io.IOException;

/**
 * Device managing 2 motors through a __MotorGroup_ providing registers to directly set center and
 * size.
 */
public class Slit extends DeviceBase {

    final Motor bladePos;
    final Motor bladeNeg;
    final Register<Double> center;
    final Register<Double> size;

    final MotorGroup motorGroup;

    Double centerValue;
    Double sizeValue;

    public Slit(String name, Motor bladePos, Motor bladeNeg) {
        this(name, bladePos, bladeNeg, null, null);
    }

    public Slit(String name, Motor bladePos, Motor bladeNeg, Register<Double> center, Register<Double> size) {
        super(name);
        this.bladePos = bladePos;
        this.bladeNeg = bladeNeg;

        if (center == null) {
            this.center = new RegisterBase<Double>(name + " center") {
                @Override
                protected Double doRead() throws InterruptedException, IOException {
                    Slit.this.doUpdate();
                    return centerValue;
                }

                @Override
                protected void doWrite(Double value) throws InterruptedException, IOException {
                    if (sizeValue == null) {
                        throw new DeviceException("Size register not initialized");
                    }
                    applyCenterAndSize(value, sizeValue);
                }

                @Override
                protected void onValueChange(Object value, Object former) {
                    if (isSimulated()) {
                        try {
                            applyCenterAndSize((Double) value, sizeValue);
                        } catch (Exception ex) {
                        }
                    }
                }
            };

        } else {
            this.center = center;
        }
        if (size == null) {
            this.size = new RegisterBase<Double>(name + " size") {
                @Override
                protected Double doRead() throws InterruptedException, IOException {
                    Slit.this.doUpdate();
                    return sizeValue;
                }

                @Override
                protected void doWrite(Double value) throws InterruptedException, IOException {
                    if (centerValue == null) {
                        throw new DeviceException("Center register not initialized");
                    }
                    applyCenterAndSize(centerValue, value);
                }

                @Override
                protected void onValueChange(Object value, Object former) {
                    if (isSimulated()) {
                        try {
                            applyCenterAndSize(centerValue, (Double) value);
                        } catch (Exception ex) {
                        }
                    }
                }
            };
        } else {
            this.size = size;
        }

        setChildren(new Device[]{bladePos, bladeNeg, this.center, this.size});
        setComponents(new Device[]{this.center, this.size});
        setTrackChildren(true);
        motorGroup = new MotorGroupBase(null, bladePos, bladeNeg);

        updateCenterAndSize();
    }

    @Override
    protected void doSetSimulated() {
        super.doSetSimulated();
        try {
            setCache((MotorBase) bladePos, 0.4);
            setCache((MotorBase) bladeNeg, 0.0);
            setCache(new double[]{0.2, 0.2});
            setCache((DeviceBase) center, 0.2);
            setCache((DeviceBase) size, 0.2);
        } catch (Exception ex) {
        }
    }

    @Override
    protected void onChildReadbackChange(Device child, Object value) {
        updateCenterAndSize();
    }

    void applyCenterAndSize(double center, double size) throws IOException, InterruptedException {
        try {
            if (size <= 0) {
                throw new DeviceException("Invalid size: " + size);
            }
            motorGroup.move(new double[]{(center + size / 2), (center - size / 2)});
        } finally {
            updateCenterAndSize();
        }
    }

    void updateCenterAndSize() {
        Double p1 = (bladePos.getReadback() == null) ? null : bladePos.getReadback().take();
        Double p2 = (bladeNeg.getReadback() == null) ? null : bladeNeg.getReadback().take();
        int precision = bladePos.getPrecision();
        if ((p1 != null) && (p2 != null)) {
            centerValue = (p1 + p2) / 2;
            sizeValue = p1 - p2;
            if (precision >= 0) {
                centerValue = Convert.roundDouble(centerValue, precision);
                sizeValue = Convert.roundDouble(sizeValue, precision);
            }
        } else {
            centerValue = Double.NaN;
            sizeValue = Double.NaN;
        }
        setCache(new double[]{centerValue, sizeValue});
        if (!isSimulated()) {
            setCache((DeviceBase) center, centerValue);
            setCache((DeviceBase) size, sizeValue);
        }
    }

    @Override
    protected void doUpdate() throws IOException, InterruptedException {
        //Do not call super.doUpdate not to update center and size, whatg would create recursive calls
        bladePos.update();
        bladeNeg.update();
        updateCenterAndSize();
    }

    public Motor getBladePos() {
        return bladePos;
    }

    public Motor getBladeNeg() {
        return bladeNeg;
    }

    public Register<Double> getCenterReg() {
        return center;
    }

    public Register<Double> getSizeReg() {
        return size;
    }

    public void setCenter(double value) throws IOException, InterruptedException {
        assertWriteEnabled();
        getCenterReg().write(value);
    }

    public void setSize(double value) throws IOException, InterruptedException {
        assertWriteEnabled();
        getSizeReg().write(value);
    }

    public void set(double center, double size) throws IOException, InterruptedException {
        assertWriteEnabled();
        applyCenterAndSize(center, size);
    }

}
