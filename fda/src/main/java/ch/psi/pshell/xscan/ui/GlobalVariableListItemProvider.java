package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Variable;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GlobalVariableListItemProvider implements ListItemProvider<Variable> {

    private List<Variable> list;

    private final String[] actions = new String[]{"Variable"};

    public GlobalVariableListItemProvider(List<Variable> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (actions);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(actions[0])) {
            Variable r = new Variable();
            list.add(r);
            return (getItem(r));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (Variable r : list) {
            l.add(getItem(r));
        }
        return l;
    }

    private Component getItem(Variable object) {
        GlobalVariablePanel p = new GlobalVariablePanel(object);
        p.setName("Variable");
        return p;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof GlobalVariablePanel globalVariablePanel) {
            list.remove(globalVariablePanel.getObject());
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
        if (component instanceof GlobalVariablePanel globalVariablePanel) {
            return (globalVariablePanel.getObject());
        }
        return null;
    }
}
