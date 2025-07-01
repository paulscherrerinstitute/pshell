package ch.psi.pshell.detector;

/**
 * Entity class holding the information about each frame received on the ZMQ stream.
 */
public class FrameInfo {

    public String dtype;
    public int[] shape;
    public String[] htype;
}
