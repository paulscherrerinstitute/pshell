package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ContinuousDimension;
import ch.psi.pshell.xscan.model.Detector;
import ch.psi.pshell.xscan.model.ScalerChannel;
import ch.psi.pshell.xscan.model.SimpleScalarDetector;
import ch.psi.pshell.xscan.model.Timestamp;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ContinuousDetectorListItemProvider implements ListItemProvider<Detector> {

    private final String[] detectors = new String[]{"Scalar Detector", "Scaler", "Timestamp"};

    private ContinuousDimension dimension;

    ContinuousDetectorListItemProvider(ContinuousDimension dimension) {
        this.dimension = dimension;
    }

    @Override
    public String[] getItemKeys() {
        // Only support up to 1 timestamp, 16 scaler channels and 8 detectors
        List<String> keys = new ArrayList<String>();
        if (dimension.getTimestamp() == null) {
            keys.add(detectors[2]);
        }
        if (dimension.getScaler().size() < 16) {
            keys.add(detectors[1]);
        }
        if (dimension.getDetector().size() < 100) {
            keys.add(detectors[0]);
        }
        return (keys.toArray(new String[keys.size()]));
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(detectors[0])) {
            SimpleScalarDetector ssd = new SimpleScalarDetector();
            dimension.getDetector().add(ssd);
            return (getItem(ssd));
        } else if (key.equals(detectors[1])) {
            ScalerChannel sc = new ScalerChannel();
            dimension.getScaler().add(sc);
            return (getItem(sc));
        } else if (key.equals(detectors[2])) {
            Timestamp td = new Timestamp();
            dimension.setTimestamp(td);
            return (getItem(td));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();

        for (SimpleScalarDetector sd : dimension.getDetector()) {
            l.add(getItem(sd));
        }

        for (ScalerChannel sd : dimension.getScaler()) {
            l.add(getItem(sd));
        }

        if (dimension.getTimestamp() != null) {
            l.add(getItem(dimension.getTimestamp()));
        }
        return l;
    }

    private Component getItem(Detector detector) {

        if (detector instanceof SimpleScalarDetector simpleScalarDetector) {
            SimpleScalarDetectorPanel p = new SimpleScalarDetectorPanel(simpleScalarDetector);
            p.setName("Scalar Detector");
            return (p);
        } else if (detector instanceof ScalerChannel scalerChannel) {
            ScalerChannelPanel p = new ScalerChannelPanel(scalerChannel);
            p.setName("Scaler");
            return (p);
        } else if (detector instanceof Timestamp timestamp) {
            TimestampDetectorPanel p = new TimestampDetectorPanel(timestamp);
            p.setName("Timestamp");
            return (p);
        }

        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof SimpleScalarDetectorPanel simpleScalarDetectorPanel) {
            SimpleScalarDetector o = simpleScalarDetectorPanel.getObject();
            dimension.getDetector().remove(o);
            ModelUtil.getInstance().findInMappingAndRemove(o);
            ModelUtil.getInstance().refreshAll();
        } else if (component instanceof ScalerChannelPanel scalerChannelPanel) {
            ScalerChannel o = scalerChannelPanel.getObject();
            dimension.getScaler().remove(o);
            ModelUtil.getInstance().findInMappingAndRemove(o);
            ModelUtil.getInstance().refreshAll();
        } else if (component instanceof TimestampDetectorPanel timestampDetectorPanel) {
            Timestamp o = timestampDetectorPanel.getObject();
            dimension.setTimestamp(null); // Remove timestamp
            ModelUtil.getInstance().findInMappingAndRemove(o);
            ModelUtil.getInstance().refreshAll();
        }
    }

    @Override
    public boolean isEmpty() {
        boolean a = dimension.getDetector().isEmpty();
        boolean b = dimension.getScaler().isEmpty();
        boolean c = dimension.getTimestamp() == null;
        return a & b & c;
//        return (getItemKeys().length==0);
    }

    @Override
    public int size() {
        int size = 0;
        size = size + dimension.getDetector().size();
        size = size + dimension.getScaler().size();
        if (dimension.getTimestamp() != null) {
            size = size + 1;
        }
        return size;
//        return getItemKeys().length;
    }

    @Override
    public void moveItemUp(Component component) {
        if (component instanceof SimpleScalarDetectorPanel simpleScalarDetectorPanel) {
            Object a = simpleScalarDetectorPanel.getObject();
            ListUtil.moveItemUp(dimension.getDetector(), a);
        } else if (component instanceof ScalerChannelPanel scalerChannelPanel) {
            Object a = scalerChannelPanel.getObject();
            ListUtil.moveItemUp(dimension.getScaler(), a);
        } else if (component instanceof TimestampDetectorPanel) {
            // ignore
        }

    }

    @Override
    public void moveItemDown(Component component) {
        if (component instanceof SimpleScalarDetectorPanel simpleScalarDetectorPanel) {
            Object a = simpleScalarDetectorPanel.getObject();
            ListUtil.moveItemDown(dimension.getDetector(), a);
        } else if (component instanceof ScalerChannelPanel scalerChannelPanel) {
            Object a = scalerChannelPanel.getObject();
            ListUtil.moveItemDown(dimension.getScaler(), a);
        } else if (component instanceof TimestampDetectorPanel) {
            // ignore
        }
    }

}
