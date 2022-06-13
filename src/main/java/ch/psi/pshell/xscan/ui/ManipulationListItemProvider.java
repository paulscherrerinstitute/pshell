package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Manipulation;
import ch.psi.pshell.xscan.model.ScriptManipulation;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ManipulationListItemProvider implements ListItemProvider<Manipulation> {

    private List<Manipulation> list;

    private final String[] names = new String[]{"Script Manipulation"};

    public ManipulationListItemProvider(List<Manipulation> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (names);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(names[0])) {
            ScriptManipulation sm = new ScriptManipulation();
            list.add(sm);
            return (getItem(sm));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (Manipulation m : list) {
            l.add(getItem(m));
        }
        return l;
    }

    private Component getItem(Manipulation object) {
        if (object instanceof ScriptManipulation) {
            ScriptManipulationPanel p = new ScriptManipulationPanel((ScriptManipulation) object);
            p.setName("Manipulation");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof ScriptManipulationPanel) {
            ScriptManipulation o = ((ScriptManipulationPanel) component).getObject();
            list.remove(o);
            ModelUtil.getInstance().findInMappingAndRemove(o);
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
        if (component instanceof ScriptManipulationPanel) {
            return ((ScriptManipulationPanel) component).getObject();
        }
        return null;
    }
}
