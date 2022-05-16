package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Configuration;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DescriptionListItemProvider implements ListItemProvider<String> {

    private final String[] items = new String[]{"Description"};

    private Configuration configuration;

    public DescriptionListItemProvider(Configuration c){
        this.configuration = c;
    }

    @Override
    public String[] getItemKeys() {

        // If no continuous dimension is specified return its key. Otherwise return no key
        // (Ensures that only one continuous dimension can be added)
        if(configuration.getDescription()==null){
            return(items);
        }
        else{
            return new String[] {};
        }
    }

    @Override
    public Component newItem(String key) {
        if(key.equals(items[0])){
            return(getItem(configuration));
        }
        return null;
    }

    @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        if(configuration.getDescription()!=null){
            l.add(getItem(configuration));
        }
        return l;
    }

    private Component getItem(Configuration object) {
        if(object instanceof Configuration){
            DescriptionPanel p = new DescriptionPanel(object);
            p.setName("");
            return(p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if(component instanceof DescriptionPanel){
            configuration.setDescription(null); // Remove description
        }
    }

    @Override
    public boolean isEmpty() {
        return (configuration.getDescription()==null);
    }

    @Override
    public int size() {
        if(configuration.getDescription()==null){
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
