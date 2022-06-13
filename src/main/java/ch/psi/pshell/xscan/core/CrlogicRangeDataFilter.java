package ch.psi.pshell.xscan.core;

public class CrlogicRangeDataFilter {

    private double motorResolution = 1;
    private int motorDirection = 1;
    private double motorOffset = 0;
    private double motorReadbackResolution = 1;
    private double motorEncoderResolution = 1;

    private double encoderOffset = 0;
    private double encoderResolution = 1;
    private int encoderDirection = 1;

    private double start = 0;
    private double end = 0;
    private boolean positive = true;

    /**
     * Marker whether the value was equal
     */
    private boolean wasEqualBefore = false;

    /**
     * @return the motorResolution
     */
    public double getMotorResolution() {
        return motorResolution;
    }

    /**
     * @param motorResolution the motorResolution to set
     */
    public void setMotorResolution(double motorResolution) {
        this.motorResolution = motorResolution;
    }

    /**
     * @return the motorDirection
     */
    public int getMotorDirection() {
        return motorDirection;
    }

    /**
     * @param motorDirection the motorDirection to set
     */
    public void setMotorDirection(int motorDirection) {
        this.motorDirection = motorDirection;
    }

    /**
     * @return the motorOffset
     */
    public double getMotorOffset() {
        return motorOffset;
    }

    /**
     * @param motorOffset the motorOffset to set
     */
    public void setMotorOffset(double motorOffset) {
        this.motorOffset = motorOffset;
    }

    /**
     * @return the motorReadbackResolution
     */
    public double getMotorReadbackResolution() {
        return motorReadbackResolution;
    }

    /**
     * @param motorReadbackResolution the motorReadbackResolution to set
     */
    public void setMotorReadbackResolution(double motorReadbackResolution) {
        this.motorReadbackResolution = motorReadbackResolution;
    }

    /**
     * @return the motorEncoderResolution
     */
    public double getMotorEncoderResolution() {
        return motorEncoderResolution;
    }

    /**
     * @param motorEncoderResolution the motorEncoderResolution to set
     */
    public void setMotorEncoderResolution(double motorEncoderResolution) {
        this.motorEncoderResolution = motorEncoderResolution;
    }

    /**
     * @return the encoderOffset
     */
    public double getEncoderOffset() {
        return encoderOffset;
    }

    /**
     * @param encoderOffset the encoderOffset to set
     */
    public void setEncoderOffset(double encoderOffset) {
        this.encoderOffset = encoderOffset;
    }

    /**
     * @return the encoderResolution
     */
    public double getEncoderResolution() {
        return encoderResolution;
    }

    /**
     * @param encoderResolution the encoderResolution to set
     */
    public void setEncoderResolution(double encoderResolution) {
        this.encoderResolution = encoderResolution;
    }

    /**
     * @return the encoderDirection
     */
    public int getEncoderDirection() {
        return encoderDirection;
    }

    /**
     * @param encoderDirection the encoderDirection to set
     */
    public void setEncoderDirection(int encoderDirection) {
        this.encoderDirection = encoderDirection;
    }

    /**
     * @return the start
     */
    public double getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(double start) {
        this.start = start;
        if (start <= end) {
            positive = true;
        } else {
            positive = false;
        }
    }

    /**
     * @return the end
     */
    public double getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(double end) {
        this.end = end;
        if (end >= start) {
            positive = true;
        } else {
            positive = false;
        }
    }

    /**
     * Calculate real position
     *
     * @param raw
     * @return
     */
    public double calculatePositionMotor(double raw) {
        return (((raw * motorResolution * motorReadbackResolution) / motorDirection) + motorOffset);
    }

    /**
     * Calculate real motor position using the readback link
     *
     * @param raw
     * @return
     */
    public double calculatePositionMotorUseReadback(double raw) {
        return ((((raw - encoderOffset) * encoderResolution * encoderDirection * motorReadbackResolution) / motorDirection) + motorOffset);
    }

    /**
     * Calculate real motor position using encoder
     *
     * @param raw
     * @return
     */
    public double calculatePositionMotorUseEncoder(double raw) {
        return (raw * motorEncoderResolution * motorReadbackResolution / motorDirection + motorOffset);
    }

    /**
     * Filter whether value is with the range
     *
     * @param value
     * @return
     */
    public boolean filter(Double value) {
        if (positive) {
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
}
