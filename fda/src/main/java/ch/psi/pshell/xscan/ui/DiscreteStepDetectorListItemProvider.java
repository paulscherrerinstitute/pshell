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
        if (object instanceof ScalarDetector scalarDetector) {
            ScalarDetectorPanel p = new ScalarDetectorPanel(scalarDetector);
            p.setName("Scalar D");
            return (p);
        } else if (object instanceof ArrayDetector arrayDetector) {
            ArrayDetectorPanel p = new ArrayDetectorPanel(arrayDetector);
            p.setName("Array D");
            return (p);
        } else if (object instanceof Timestamp timestamp) {
            TimestampDetectorPanel p = new TimestampDetectorPanel(timestamp);
            p.setName("Timestamp");
            return (p);
        } else if (object instanceof DetectorOfDetectors detectorOfDetectors) {
            DetectorOfDetectorsPanel p = new DetectorOfDetectorsPanel(detectorOfDetectors);
            p.setName("D of D");
            return (p);
        }

        return null;
    }

    @Override
    public void removeItem(Component component) {
        Detector o = null;
        if (component instanceof ScalarDetectorPanel scalarDetectorPanel) {
            o = scalarDetectorPanel.getObject();
            list.remove(o);
        } else if (component instanceof ArrayDetectorPanel arrayDetectorPanel) {
            o = arrayDetectorPanel.getObject();
            list.remove(o);
        } else if (component instanceof TimestampDetectorPanel timestampDetectorPanel) {
            o = timestampDetectorPanel.getObject();
            list.remove(o);
        } else if (component instanceof DetectorOfDetectorsPanel detectorOfDetectorsPanel) {
            o = detectorOfDetectorsPanel.getObject();
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
        if (component instanceof ScalarDetectorPanel scalarDetectorPanel) {
            return scalarDetectorPanel.getObject();
        } else if (component instanceof ArrayDetectorPanel arrayDetectorPanel) {
            return arrayDetectorPanel.getObject();
        } else if (component instanceof TimestampDetectorPanel timestampDetectorPanel) {
            return timestampDetectorPanel.getObject();
        } else if (component instanceof DetectorOfDetectorsPanel detectorOfDetectorsPanel) {
            return detectorOfDetectorsPanel.getObject();
        }
        return null;
    }

}
