package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ArrayDetector;
import ch.psi.pshell.xscan.model.Detector;
import ch.psi.pshell.xscan.model.DetectorOfDetectors;
import ch.psi.pshell.xscan.model.ScalarDetector;
import ch.psi.pshell.xscan.model.Timestamp;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DiscreteStepDetectorListItemProvider implements ListItemProvider<Detector> {

    private List<Detector> list;

    private final String[] detectors = new String[]{"Scalar Detector", "Array Detector", "Timestamp", "Detector of Detectors"};

    public DiscreteStepDetectorListItemProvider(List<Detector> list) {
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
        } else if (key.equals(detectors[1])) {
            ArrayDetector ad = new ArrayDetector();
            list.add(ad);
            return (getItem(ad));
        } else if (key.equals(detectors[2])) {
            Timestamp t = new Timestamp();
            list.add(t);
            return (getItem(t));
        } else if (key.equals(detectors[3])) {
            DetectorOfDetectors dod = new DetectorOfDetectors();
            list.add(dod);
            return (getItem(dod));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (Detector d : list) {
            l.add(getItem(d));
        }
        return l;
    }

    private Component getItem(Detector object) {
        if (object instanceof ScalarDetector) {
            ScalarDetectorPanel p = new ScalarDetectorPanel((ScalarDetector) object);
            p.setName("Scalar D");
            return (p);
        } else if (object instanceof ArrayDetector) {
            ArrayDetectorPanel p = new ArrayDetectorPanel((ArrayDetector) object);
            p.setName("Array D");
            return (p);
        } else if (object instanceof Timestamp) {
            TimestampDetectorPanel p = new TimestampDetectorPanel((Timestamp) object);
            p.setName("Timestamp");
            return (p);
        } else if (object instanceof DetectorOfDetectors) {
            DetectorOfDetectorsPanel p = new DetectorOfDetectorsPanel((DetectorOfDetectors) object);
            p.setName("D of D");
            return (p);
        }

        return null;
    }

    @Override
    public void removeItem(Component component) {
        Detector o = null;
        if (component instanceof ScalarDetectorPanel) {
            o = ((ScalarDetectorPanel) component).getObject();
            list.remove(o);
        } else if (component instanceof ArrayDetectorPanel) {
            o = ((ArrayDetectorPanel) component).getObject();
            list.remove(o);
        } else if (component instanceof TimestampDetectorPanel) {
            o = ((TimestampDetectorPanel) component).getObject();
            list.remove(o);
        } else if (component instanceof DetectorOfDetectorsPanel) {
            o = ((DetectorOfDetectorsPanel) component).getObject();
            list.remove(o);
        }

        // Find references to this object and remove the reference
        if (o != null) {
            ModelUtil.getInstance().findInMappingAndRemove(o);
            ModelUtil.getInstance().refreshAll();
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
            return ((ScalarDetectorPanel) component).getObject();
        } else if (component instanceof ArrayDetectorPanel) {
            return ((ArrayDetectorPanel) component).getObject();
        } else if (component instanceof TimestampDetectorPanel) {
            return ((TimestampDetectorPanel) component).getObject();
        } else if (component instanceof DetectorOfDetectorsPanel) {
            return ((DetectorOfDetectorsPanel) component).getObject();
        }
        return null;
    }

}
