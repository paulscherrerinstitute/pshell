package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.LinePlot;
import ch.psi.pshell.xscan.model.LinePlotArray;
import ch.psi.pshell.xscan.model.MatrixPlot;
import ch.psi.pshell.xscan.model.MatrixPlotArray;
import ch.psi.pshell.xscan.model.Visualization;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VisualizationListItemProvider implements ListItemProvider<Visualization> {

    private final String[] dimensions = new String[]{"Line Plot", "Matrix Plot", "Line Plot Array", "Matrix Plot Array"};

    private List<Visualization> list;

    public VisualizationListItemProvider(List<Visualization> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (dimensions);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(dimensions[0])) {
            LinePlot lp = new LinePlot();
            list.add(lp);
            return (getItem(lp));
        } else if (key.equals(dimensions[1])) {
            MatrixPlot mp = new MatrixPlot();
            list.add(mp);
            return (getItem(mp));
        } else if (key.equals(dimensions[2])) {
            LinePlotArray lp = new LinePlotArray();
            list.add(lp);
            return (getItem(lp));
        } else if (key.equals(dimensions[3])) {
            MatrixPlotArray mp = new MatrixPlotArray();
            list.add(mp);
            return (getItem(mp));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (Visualization v : list) {
            Component c = getItem(v);
            if (c != null) {
                l.add(c);
            }
        }
        return l;
    }

    private Component getItem(Visualization object) {
        if (object instanceof LinePlot) {
            LinePlotPanel p = new LinePlotPanel((LinePlot) object);
            p.setName("Line P");
            return (p);
        } else if (object instanceof MatrixPlot) {
            MatrixPlotPanel p = new MatrixPlotPanel((MatrixPlot) object);
            p.setName("Matrix P");
            return (p);
        } else if (object instanceof LinePlotArray) {
            LinePlotArrayPanel p = new LinePlotArrayPanel((LinePlotArray) object);
            p.setName("Line P Array");
            return (p);
        } else if (object instanceof MatrixPlotArray) {
            MatrixPlotArrayPanel p = new MatrixPlotArrayPanel((MatrixPlotArray) object);
            p.setName("Matrix P Array");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof LinePlotPanel) {
            list.remove(((LinePlotPanel) component).getObject());
        } else if (component instanceof MatrixPlotPanel) {
            list.remove(((MatrixPlotPanel) component).getObject());
        } else if (component instanceof LinePlotArrayPanel) {
            list.remove(((LinePlotArrayPanel) component).getObject());
        } else if (component instanceof MatrixPlotArrayPanel) {
            list.remove(((MatrixPlotArrayPanel) component).getObject());
        }
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void moveItemUp(Component component) {
        ListUtil.moveItemUp(list, getObject(component));
    }

    @Override
    public void moveItemDown(Component component) {
        ListUtil.moveItemDown(list, getObject(component));
    }

    private Object getObject(Component component) {
        if (component instanceof LinePlotPanel) {
            return (((LinePlotPanel) component).getObject());
        } else if (component instanceof MatrixPlotPanel) {
            return (((MatrixPlotPanel) component).getObject());
        } else if (component instanceof LinePlotArrayPanel) {
            return (((LinePlotArrayPanel) component).getObject());
        } else if (component instanceof MatrixPlotArrayPanel) {
            return (((MatrixPlotArrayPanel) component).getObject());
        }
        return null;
    }
}
