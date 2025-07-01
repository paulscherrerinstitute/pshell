package ch.psi.pshell.device;

/**
 *
 */
public class MasterPositionerConfig extends PositionerConfig {

    public static enum MODE {
        LINEAR
    }

    public MODE mode = MODE.LINEAR;

    public double[] masterPositions;
    public double[] slave1Positions;
    public double[] slave2Positions;
    public double[] slave3Positions;
    public double[] slave4Positions;
    public double[] slave5Positions;
    public double[] slave6Positions;
}
