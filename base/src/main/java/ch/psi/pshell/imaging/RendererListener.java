package ch.psi.pshell.imaging;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * The listener interface for receiving image renderer events.
 */
public interface RendererListener {

    //From ImageListener
    public default void onImage(ImageRenderer renderer, Object origin, BufferedImage image, Data data) {
    }

    public default void onError(ImageRenderer renderer, Object origin, Exception ex) {
    }

    //Mouse events
    public default void onMousePressed(ImageRenderer renderer, Point p) {
    }

    public default void onMouseReleased(ImageRenderer renderer, Point p) {
    }

    public default void onMouseMoved(ImageRenderer renderer, Point p) {
    }

    public default void onMouseDragged(ImageRenderer renderer, Point p) {
    }

    public default void onMouseDoubleClick(ImageRenderer renderer, Point p) {
    }

    //Overlay events
    public default void onMoveStarted(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onMoving(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onMoveFinished(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onMoveAborted(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onSelecting(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onSelectionStarted(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onSelectionFinished(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onSelectionAborted(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onSelectedOverlayChanged(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onDeleted(ImageRenderer renderer, Overlay overlay) {
    }

    public default void onInserted(ImageRenderer renderer, Overlay overlay) {
    }

}
