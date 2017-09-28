package ch.psi.pshell.plotter;

import ch.psi.pshell.plot.LinePlotJFree;
import ch.psi.pshell.plot.MatrixPlotJFree;

/**
 *
 */
public enum PlotType {
    Line,
    Matrix;

    Class getImplementation() {
        switch (this) {
            case Line:
                return LinePlotJFree.class;
            case Matrix:
                return MatrixPlotJFree.class;
            default:
                return null;
        }
    }
}
