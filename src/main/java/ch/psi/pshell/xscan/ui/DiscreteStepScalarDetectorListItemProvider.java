package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ScalarDetector;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DiscreteStepScalarDetectorListItemProvider implements ListItemProvider<ScalarDetector> {

    private List<ScalarDetector> list;

    private final String[] detectors = new String[]{"Scalar Detector"};

    public DiscreteStepScalarDetectorListItemProvider(List<ScalarDetector> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (detectors);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(detectors[0])) {
            ScalarDetector sd = new ScalarDetector();
            list.add(sd);
            return (getItem(sd));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (ScalarDetector s : list) {
            l.add(getItem(s));
        }
        return l;
    }

    private Component getItem(ScalarDetector object) {
        if (object instanceof ScalarDetector) {
            ScalarDetectorPanel p = new ScalarDetectorPanel(object);
            p.setName("Scalar D");
            return (p);
        }

        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof ScalarDetectorPanel) {
            list.remove(((ScalarDetectorPanel) component).getObject());
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
        if (component instanceof ScalarDetectorPanel) {
            return (((ScalarDetectorPanel) component).getObject());
        }
        return null;
    }
}
