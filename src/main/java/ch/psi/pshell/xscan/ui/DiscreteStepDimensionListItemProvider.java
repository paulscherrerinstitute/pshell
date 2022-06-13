package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.DiscreteStepDimension;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DiscreteStepDimensionListItemProvider implements ListItemProvider<DiscreteStepDimension> {

    private List<DiscreteStepDimension> list;

    private final String[] dimensions = new String[]{"Dimension"};

    public DiscreteStepDimensionListItemProvider(List<DiscreteStepDimension> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (dimensions);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(dimensions[0])) {
            DiscreteStepDimension dsd = new DiscreteStepDimension();
            list.add(dsd);
            return (getItem(dsd));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (DiscreteStepDimension d : list) {
            l.add(getItem(d));
        }
        return l;
    }

    private Component getItem(DiscreteStepDimension object) {
        if (object instanceof DiscreteStepDimension) {
            DiscreteStepDimensionPanel p = new DiscreteStepDimensionPanel(object);
            p.setName("D");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof DiscreteStepDimensionPanel) {
            list.remove(((DiscreteStepDimensionPanel) component).getObject());
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
        if (component instanceof DiscreteStepDimensionPanel) {
            return (((DiscreteStepDimensionPanel) component).getObject());
        }
        return null;
    }

}
