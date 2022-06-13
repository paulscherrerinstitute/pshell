package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.DiscreteStepDimension;
import ch.psi.pshell.xscan.model.Guard;
import ch.psi.pshell.xscan.model.GuardCondition;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GuardListItemProvider implements ListItemProvider<GuardCondition> {

    DiscreteStepDimension dimension;

    private final String[] positioners = new String[]{"Guard"};

    public GuardListItemProvider(DiscreteStepDimension dimension) {
        this.dimension = dimension;
    }

    @Override
    public String[] getItemKeys() {
        return (positioners);
    }

    @Override
    public Component newItem(String key) {
        if (dimension.getGuard() == null) {
            dimension.setGuard(new Guard());
        }

        if (key.equals(positioners[0])) {
            GuardCondition gc = new GuardCondition();
            dimension.getGuard().getCondition().add(gc);
            return (getItem(gc));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        if (dimension.getGuard() != null) {
            for (GuardCondition c : dimension.getGuard().getCondition()) {
                l.add(getItem(c));
            }
        }
        return l;
    }

    private Component getItem(GuardCondition object) {
        if (object instanceof GuardCondition) {
            GuardConditionPanel p = new GuardConditionPanel(object);
            p.setName("Guard");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof GuardConditionPanel) {
            GuardCondition c = ((GuardConditionPanel) component).getObject();
            if (dimension.getGuard() != null) {
                dimension.getGuard().getCondition().remove(c);

                // Remove guard from dimension if there is no condition left
                if (dimension.getGuard().getCondition().isEmpty()) {
                    dimension.setGuard(null);
                }
            }
            // There is nothing to be removed
        }
    }

    @Override
    public boolean isEmpty() {
        if (dimension.getGuard() != null) {
            return dimension.getGuard().getCondition().isEmpty();
        }
        return true;
    }

    @Override
    public int size() {
        if (dimension.getGuard() != null) {
            return dimension.getGuard().getCondition().size();
        }
        return 0;
    }

    @Override
    public void moveItemUp(Component component) {
        ListUtil.moveItemUp(dimension.getGuard().getCondition(), getObject(component));
    }

    @Override
    public void moveItemDown(Component component) {
        ListUtil.moveItemDown(dimension.getGuard().getCondition(), getObject(component));
    }

    private Object getObject(Component component) {
        if (component instanceof GuardConditionPanel) {
            return ((GuardConditionPanel) component).getObject();
        }
        return null;
    }
}
