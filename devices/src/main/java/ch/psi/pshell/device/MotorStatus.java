package ch.psi.pshell.device;

/**
 * Entity class holding the current flags of a motor.
 */
public class MotorStatus {

    public boolean onHardLimitSup;
    public boolean onHardLimitInf;
    public boolean onSoftLimitSup;
    public boolean onSoftLimitInf;
    public boolean referencing;
    public boolean referenced;
    public boolean stopped;
    public boolean enabled;
    public boolean error;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MotorStatus ms) {            
            return ((onHardLimitSup == ms.onHardLimitSup)
                    && (onHardLimitInf == ms.onHardLimitInf)
                    && (onSoftLimitSup == ms.onSoftLimitSup)
                    && (onSoftLimitInf == ms.onSoftLimitInf)
                    && (stopped == ms.stopped)
                    && (enabled == ms.enabled));
        }
        return false;
    }

    @Override
    public String toString() {
        if (!enabled) {
            return "Disabled";
        }
        if (error) {
            return "Error";
        }
        if (referencing) {
            return "Referencing";
        }
        if (!referenced) {
            return "Not referenced";
        }
        if (onHardLimitInf) {
            return "Hard low limit";
        }
        if (onHardLimitSup) {
            return "Hard high limit";
        }
        if (onSoftLimitInf) {
            return "Soft low limit";
        }
        if (onSoftLimitSup) {
            return "Soft high limit";
        }
        if (stopped) {
            return "Stopped";
        }
        return "Moving";
    }
}
