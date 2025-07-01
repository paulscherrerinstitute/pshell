package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ChannelParameterMapping;
import ch.psi.pshell.xscan.model.IDParameterMapping;
import ch.psi.pshell.xscan.model.ParameterMapping;
import ch.psi.pshell.xscan.model.VariableParameterMapping;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ParameterMappingListItemProvider implements ListItemProvider<ParameterMapping> {

    private List<ParameterMapping> list;

    private final String[] names = new String[]{"ID Mapping", "Channel Mapping", "Variable Mapping"};

    public ParameterMappingListItemProvider(List<ParameterMapping> list) {
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return (names);
    }

    @Override
    public Component newItem(String key) {
        if (key.equals(names[0])) {
            IDParameterMapping pm = new IDParameterMapping();
            list.add(pm);
            return (getItem(pm));
        } else if (key.equals(names[1])) {
            ChannelParameterMapping cpm = new ChannelParameterMapping();
            list.add(cpm);
            return (getItem(cpm));
        } else if (key.equals(names[2])) {
            VariableParameterMapping cpm = new VariableParameterMapping();
            list.add(cpm);
            return (getItem(cpm));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for (ParameterMapping m : list) {
            l.add(getItem(m));
        }
        return l;
    }

    private Component getItem(ParameterMapping object) {
        if (object instanceof IDParameterMapping map) {
            IDParameterMappingPanel p = new IDParameterMappingPanel(map);
            p.setName("ID Mapping");
            return (p);
        } else if (object instanceof ChannelParameterMapping map) {
            ChannelParameterMappingPanel p = new ChannelParameterMappingPanel(map);
            p.setName("Channel Mapping");
            return (p);
        } else if (object instanceof VariableParameterMapping map) {
            GlobalVariableParameterMappingPanel p = new GlobalVariableParameterMappingPanel(map);
            p.setName("Variable Mapping");
            return (p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if (component instanceof IDParameterMappingPanel map) {
            list.remove(map.getObject());
        } else if (component instanceof ChannelParameterMappingPanel map) {
            list.remove(map.getObject());
        } else if (component instanceof GlobalVariableParameterMappingPanel map) {
            list.remove(map.getObject());
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
        if (component instanceof IDParameterMappingPanel map) {
            return map.getObject();
        } else if (component instanceof ChannelParameterMappingPanel map) {
            return map.getObject();
        } else if (component instanceof GlobalVariableParameterMappingPanel map) {
            return map.getObject();
        }
        return null;
    }
}
