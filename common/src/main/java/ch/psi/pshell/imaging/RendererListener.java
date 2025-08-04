package ch.psi.pshell.imaging;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * The listener interface for receiving image renderer events.
 */
public interface RendererListener {

    //From ImageListener
    public default void onImage(Renderer renderer, Object origin, BufferedImage image, Data data) {
    }

    public default void onError(Renderer renderer, Object origin, Exception ex) {
    }

    //Mouse events
    public default void onMousePressed(Renderer renderer, Point p) {
    }

    public default void onMouseReleased(Renderer renderer, Point p) {
    }

    public default void onMouseMoved(Renderer renderer, Point p) {
    }

    public default void onMouseDragged(Renderer renderer, Point p) {
    }

    public default void onMouseDoubleClick(Renderer renderer, Point p) {
    }

    //Overlay events
    public default void onMoveStarted(Renderer renderer, Overlay overlay) {
    }

    public default void onMoving(Renderer renderer, Overlay overlay) {
    }

    public default void onMoveFinished(Renderer renderer, Overlay overlay) {
    }

    public default void onMoveAborted(Renderer renderer, Overlay overlay) {
    }

    public default void onSelecting(Renderer renderer, Overlay overlay) {
    }

    public default void onSelectionStarted(Renderer renderer, Overlay overlay) {
    }

    public default void onSelectionFinished(Renderer renderer, Overlay overlay) {
    }

    public default void onSelectionAborted(Renderer renderer, Overlay overlay) {
    }

    public default void onSelectedOverlayChanged(Renderer renderer, Overlay overlay) {
    }

    public default void onDeleted(Renderer renderer, Overlay overlay) {
    }

    public default void onInserted(Renderer renderer, Overlay overlay) {
    }

}
