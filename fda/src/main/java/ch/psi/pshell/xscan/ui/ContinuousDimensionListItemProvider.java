package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ContinuousDimension;
import ch.psi.pshell.xscan.model.Scan;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
//import org.openide.util.Lookup;

/**
 *
 */
public class ContinuousDimensionListItemProvider implements ListItemProvider<ContinuousDimension> {

    private final String[] dimensions = new String[]{"Continous Dimension"};

    private Scan scan;

    public ContinuousDimensionListItemProvider(Scan scan) {
        this.scan = scan;
    }

    @Override
    public String[] getItemKeys() {

        ///TODO: 
        // Only show this option if OTFSCAN is configured - workaround
        //ExecutionService eservice = Lookup.getDefault().lookup(ExecutionService.class);
        //if(eservice != null && ! eservice.supportsFeature("ch.psi.aq.feature.otfscan")){
        //    return new String[]{};
        //}
        // If no continuous dimension is specified return its key. Otherwise return no key
        // (Ensures that only one continuous dimension can be added)
        if (scan.getCdimension() == null) {
            return (dimensions);
        } else {
            return new String[]{};
        }
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(dimensions[0])) {
            ContinuousDimension d = new ContinuousDimension();
            scan.setCdimension(d);
            return (getItem(d));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        if (scan.getCdimension() != null) {
            l.add(getItem(scan.getCdimension()));
        }
        return l;
    }

    private Component getItem(ContinuousDimension object) {
        if (object instanceof ContinuousDimension) {
            ContinuousDimensionPanel p = new ContinuousDimensionPanel(object);
            p.setName("D");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof ContinuousDimensionPanel continuousDimensionPanel) {
            ContinuousDimension d = continuousDimensionPanel.getObject();
            ModelUtil.getInstance().findInMappingAndRemove(d.getPositioner());
            scan.setCdimension(null); // Remove continuous dimension from scan
        }
    }

    @Override
    public boolean isEmpty() {
        return (scan.getCdimension() == null);
    }

    @Override
    public int size() {
        if (scan.getCdimension() == null) {
            return 0;
        }
        return 1;
    }

    @Override
    public void moveItemUp(Component component) {
        // Not supported as there is only one continuous dimension
    }

    @Override
    public void moveItemDown(Component component) {
        // Not supported as there is only one continuous dimension
    }
}
