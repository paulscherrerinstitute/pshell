package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Function;
import ch.psi.pshell.xscan.model.Region;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FunctionListItemProvider implements ListItemProvider<Function> {

    private final String[] items = new String[]{"Function"};

    private Region region;

    public FunctionListItemProvider(Region region) {
        this.region = region;
    }

    @Override
    public String[] getItemKeys() {

        // If no continuous dimension is specified return its key. Otherwise return no key
        // (Ensures that only one continuous dimension can be added)
        if (region.getFunction() == null) {
            return (items);
        } else {
            return new String[]{};
        }
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(items[0])) {
            Function d = new Function();
            region.setFunction(d);
            return (getItem(d));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        if (region.getFunction() != null) {
            l.add(getItem(region.getFunction()));
        }
        return l;
    }

    private Component getItem(Function object) {
        if (object instanceof Function) {
            FunctionPanel p = new FunctionPanel(object);
            p.setName("");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof FunctionPanel) {
            region.setFunction(null); // Remove function from region
        }
    }

    @Override
    public boolean isEmpty() {
        return (region.getFunction() == null);
    }

    @Override
    public int size() {
        if (region.getFunction() == null) {
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
