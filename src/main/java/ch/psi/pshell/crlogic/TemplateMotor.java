package ch.psi.pshell.crlogic;

import ch.psi.jcae.Channel;
import ch.psi.jcae.annotation.CaChannel;

public class TemplateMotor {

    public enum Type {
        SOFT_CHANNEL, MOTOR_SIMULATION, OMS_VME58, OMS_MAXv
    };

    /**
     * ## Drive section ## # User coordinates #
     */
    /**
     * .HLM	High limit	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.HLM")
    private Channel<Double> highLimit;

    /**
     * .LLM	Low limit	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.LLM")
    private Channel<Double> lowLimit;

    /**
     * .RBV	Readback value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.RBV", monitor = true)
    private Channel<Double> readbackValue;

    /**
     * .VAL	Set value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.VAL", monitor = true)
    private Channel<Double> setValue;

    /**
     * .RLV	Relative move value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.RLV")
    private Channel<Double> relativeMoveValue;

    /**
     * .TWV	Teak value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.TWV")
    private Channel<Double> tweakValue;

    /**
     * .TWR	Tweak reverse - move left	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.TWR")
    private Channel<Integer> tweakReverse;

    /**
     * .TWF	Tweak forward - move right	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.TWF")
    private Channel<Integer> tweakForward;

    /**
     * .JOGR	Jog reverse	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.JOGR")
    private Channel<Integer> jogReverse;

    /**
     * .JOGF	Jog forward	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.JOGF")
    private Channel<Integer> jogForward;

    /**
     * .HOMR	Home reverse	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.HOMR")
    private Channel<Integer> homeReverse;

    /**
     * .HOMF	Home forward	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.HOMF")
    private Channel<Integer> homeForward;

    /**
     * .EGU	Engineering unit	- String
     */
    @CaChannel(type = String.class, name = "${PREFIX}.EGU")
    private Channel<String> engineeringUnit;

    /**
     * .DTYP	Type	- String (e.g. "OMS MAXv") see enum Type
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.DTYP")
    private Channel<Integer> type;

    /**
     * .DESC	Description	- String
     */
    @CaChannel(type = String.class, name = "${PREFIX}.DESC")
    private Channel<String> description;

    /**
     * # Dial coordinates #
     */
    /**
     * .DHLM	Dial high limit	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.DHLM")
    private Channel<Double> dialHighLimit;

    /**
     * .DLLM	Dial low limit	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.DLLM")
    private Channel<Double> dialLowLimit;

    /**
     * .DRBV	Dial readback value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.DRBV", monitor = true)
    private Channel<Double> dialReadbackValue;

    /**
     * .DVAL	Dial set value	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.DVAL", monitor = true)
    private Channel<Double> dialSetValue;

    /**
     * .RVAL	Raw value	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.RVAL", monitor = true)
    private Channel<Integer> rawValue;

    /**
     * .RRBV	Raw readback value	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.RRBV", monitor = true)
    private Channel<Integer> rawReadbackValue;

    /**
     * .SPMG	Stop/Pause/Move/Go	- (0:"Stop", 1:"Pause", 2:"Move", 3:"Go")	- 3
     */
    public enum Commands {
        Stop, Pause, Move, Go
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}.SPMG")
    private Channel<Integer> command;

    /**
     * ## Calibration section ##
     */
    /**
     * .SET	Set/Use Switch	- (0:"Use", 1:"Set")	- 0
     */
    public enum Calibration {
        Use, Set
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}.SET")
    private Channel<Integer> calibration;

    /**
     * .OFF	User offset (EGU)	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.OFF")
    private Channel<Double> offset;

    /**
     * .FOFF	Offset-Freeze Switch	- (0:"Variable", 1:"Frozen") - 1
     */
    public enum OffsetMode {
        Variable, Frozen
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}.FOFF")
    private Channel<Integer> offsetMode;

    /**
     * .DIR	User direction	- (0:"Pos", 1:"Neg")
     */
    public enum Direction {
        Positive, Negative
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}.DIR")
    private Channel<Integer> direction;

    /**
     * ## Dynamics ##
     */
    /**
     * .VELO	Velocity (EGU/s)	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.VELO")
    private Channel<Double> velocity;

    /**
     * .BVEL	Backlash velocity (EGU/s) - double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.BVEL")
    private Channel<Double> backlashVelocity;

    /**
     * .VBAS	Base speed (EGU/s) - double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.VBAS")
    private Channel<Double> baseSpeed;

    /**
     * .ACCL	Acceleration time / seconds to velocity	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.ACCL")
    private Channel<Double> accelerationTime;

    /**
     * .BACC	Backlash acceleration time / seconds to velocity	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.BACC")
    private Channel<Double> backlashAccelerationTime;

    /**
     * .BDST	Backlash distance (EGU)	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.BDST")
    private Channel<Double> backlashDistance;

    /**
     * .FRAC	Move fraction	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.FRAC")
    private Channel<Double> moveFracion;

    /**
     * ## Resolution ##
     */
    /**
     * .MRES	Motor resolution	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.MRES")
    private Channel<Double> motorResolution;

    /**
     * .ERES	Encoder resolution	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.ERES")
    private Channel<Double> encoderResolution;

    /**
     * .RRES	Readback resolution	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.RRES")
    private Channel<Double> readbackResolution;

    /**
     * .RDBD	Retry deadband (EGU)	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.RDBD")
    private Channel<Double> retryDeadband;

    /**
     * .RTRY	Max retry count	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.RTRY")
    private Channel<Integer> maxRetryCount;

    /**
     * .RCNT	Retry count	- int
     */
    @CaChannel(type = Integer.class, name = "${PREFIX}.RCNT", monitor = true)
    private Channel<Integer> retryCount;

    /**
     * .UEIP	Use encoder (if present) - (0:"No", 1:"Yes")
     */
    @CaChannel(type = Boolean.class, name = "${PREFIX}.UEIP")
    private Channel<Boolean> useEncoder;

    /**
     * .URIP	Use readback link (if present)	- (0:"No", 1:"Yes")
     */
    @CaChannel(type = Boolean.class, name = "${PREFIX}.URIP")
    private Channel<Boolean> useReadback;

    /**
     * .DLY	Readback delay (s)	- double
     */
    @CaChannel(type = Double.class, name = "${PREFIX}.DLY")
    private Channel<Double> readbackDelay;

    /**
     * .RDBL	Readback link	- String
     */
    @CaChannel(type = String.class, name = "${PREFIX}.RDBL")
    private Channel<String> readbackLink;

    /**
     * .OMSL	Output mode select	- (0:"supervisory", 1:"closed_loop")
     */
    public enum OutputMode {
        Supervisory, Closed_Loop
    };
    @CaChannel(type = Integer.class, name = "${PREFIX}.OMSL")
    private Channel<Integer> outputMode;

    /**
     * ## Status ##
     */
    /**
     * .DMOV	Done move	- int
     */
    @CaChannel(type = Boolean.class, name = "${PREFIX}.DMOV", monitor = true)
    private Channel<Boolean> moveDone;

    public Channel<Double> getHighLimit() {
        return highLimit;
    }

    public Channel<Double> getLowLimit() {
        return lowLimit;
    }

    public Channel<Double> getReadbackValue() {
        return readbackValue;
    }

    public Channel<Double> getSetValue() {
        return setValue;
    }

    public Channel<Double> getRelativeMoveValue() {
        return relativeMoveValue;
    }

    public Channel<Double> getTweakValue() {
        return tweakValue;
    }

    public Channel<Integer> getTweakReverse() {
        return tweakReverse;
    }

    public Channel<Integer> getTweakForward() {
        return tweakForward;
    }

    public Channel<Integer> getJogReverse() {
        return jogReverse;
    }

    public Channel<Integer> getJogForward() {
        return jogForward;
    }

    public Channel<Integer> getHomeReverse() {
        return homeReverse;
    }

    public Channel<Integer> getHomeForward() {
        return homeForward;
    }

    public Channel<String> getEngineeringUnit() {
        return engineeringUnit;
    }

    public Channel<Integer> getType() {
        return type;
    }

    public Channel<String> getDescription() {
        return description;
    }

    public Channel<Double> getDialHighLimit() {
        return dialHighLimit;
    }

    public Channel<Double> getDialLowLimit() {
        return dialLowLimit;
    }

    public Channel<Double> getDialReadbackValue() {
        return dialReadbackValue;
    }

    public Channel<Double> getDialSetValue() {
        return dialSetValue;
    }

    public Channel<Integer> getRawValue() {
        return rawValue;
    }

    public Channel<Integer> getRawReadbackValue() {
        return rawReadbackValue;
    }

    public Channel<Integer> getCommand() {
        return command;
    }

    public Channel<Integer> getCalibration() {
        return calibration;
    }

    public Channel<Double> getOffset() {
        return offset;
    }

    public Channel<Integer> getOffsetMode() {
        return offsetMode;
    }

    public Channel<Integer> getDirection() {
        return direction;
    }

    public Channel<Double> getVelocity() {
        return velocity;
    }

    public Channel<Double> getBacklashVelocity() {
        return backlashVelocity;
    }

    public Channel<Double> getBaseSpeed() {
        return baseSpeed;
    }

    public Channel<Double> getAccelerationTime() {
        return accelerationTime;
    }

    public Channel<Double> getBacklashAccelerationTime() {
        return backlashAccelerationTime;
    }

    public Channel<Double> getBacklashDistance() {
        return backlashDistance;
    }

    public Channel<Double> getMoveFracion() {
        return moveFracion;
    }

    public Channel<Double> getMotorResolution() {
        return motorResolution;
    }

    public Channel<Double> getEncoderResolution() {
        return encoderResolution;
    }

    public Channel<Double> getReadbackResolution() {
        return readbackResolution;
    }

    public Channel<Double> getRetryDeadband() {
        return retryDeadband;
    }

    public Channel<Integer> getMaxRetryCount() {
        return maxRetryCount;
    }

    public Channel<Integer> getRetryCount() {
        return retryCount;
    }

    public Channel<Boolean> getUseEncoder() {
        return useEncoder;
    }

    public Channel<Boolean> getUseReadback() {
        return useReadback;
    }

    public Channel<Double> getReadbackDelay() {
        return readbackDelay;
    }

    public Channel<String> getReadbackLink() {
        return readbackLink;
    }

    public Channel<Integer> getOutputMode() {
        return outputMode;
    }

    public Channel<Boolean> getMoveDone() {
        return moveDone;
    }

}
