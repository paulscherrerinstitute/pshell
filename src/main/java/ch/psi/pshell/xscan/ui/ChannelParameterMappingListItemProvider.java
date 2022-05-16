package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ChannelParameterMapping;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ChannelParameterMappingListItemProvider implements ListItemProvider<ChannelParameterMapping> {

    private List<ChannelParameterMapping> list;

    private final String[] names = new String[]{"Channel Mapping"};

    public ChannelParameterMappingListItemProvider(List<ChannelParameterMapping> list){
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return(names);
    }

    @Override
    public Component newItem(String key) {
        if(key.equals(names[0])){
            ChannelParameterMapping mapping = new ChannelParameterMapping();
            list.add(mapping);
            return(getItem(mapping));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for(ChannelParameterMapping m: list){
            l.add(getItem(m));
        }
        return(l);
    }

    private Component getItem(ChannelParameterMapping mapping){
        if(mapping instanceof ChannelParameterMapping){
            ChannelParameterMappingPanel p = new ChannelParameterMappingPanel(mapping);
            p.setName("Channel Mapping");
            return(p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if(component instanceof ChannelParameterMappingPanel){
            list.remove(((ChannelParameterMappingPanel)component).getObject());
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

    private Object getObject(Component component){
        if(component instanceof ChannelParameterMappingPanel){
            return (((ChannelParameterMappingPanel)component).getObject());
        }
        return null;
    }
}
