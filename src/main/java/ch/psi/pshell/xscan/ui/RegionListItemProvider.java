package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Region;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RegionListItemProvider implements ListItemProvider<Region> {

    private List<Region> list;

    private final String[] actions = new String[]{"Region"};

    public RegionListItemProvider(List<Region> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (actions);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(actions[0])) {
            Region r = new Region();
            list.add(r);
            return (getItem(r));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (Region r : list) {
            l.add(getItem(r));
        }
        return l;
    }

    private Component getItem(Region object) {
        RegionPanel p = new RegionPanel(object);
        p.setName("Region");
        return p;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof RegionPanel) {
            list.remove(((RegionPanel) component).getObject());
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
        if (component instanceof RegionPanel) {
            return (((RegionPanel) component).getObject());
        }
        return null;
    }
}
