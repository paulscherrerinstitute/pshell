package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.Action;
import ch.psi.pshell.xscan.model.ChannelAction;
import ch.psi.pshell.xscan.model.ScriptAction;
import ch.psi.pshell.xscan.model.ShellAction;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ActionListItemProvider implements ListItemProvider<Action> {

    private final String[] actions = new String[]{"Shell Action", "Channel Action", "Script Action"};

    private List<Action> list;

    public ActionListItemProvider(List<Action> list){
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return(actions);
    }

    @Override
    public Component newItem(String key) {
        if(key.equals(actions[0])){
            ShellAction action = new ShellAction();
            list.add(action);
            return (getItem(action));
        }
        else if(key.equals(actions[1])){
            ChannelAction action = new ChannelAction();
            list.add(action);
            return (getItem(action));
        }
        else if(key.equals(actions[2])){
            ScriptAction action = new ScriptAction();
            list.add(action);
            return (getItem(action));
        }
        
        return null;
    }



     @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for(Action a: list){
            l.add(getItem(a));
        }
        return l;
    }

    private Component getItem(Action object){
        if(object instanceof ShellAction){
            ShellActionPanel p = new ShellActionPanel((ShellAction)object);
            p.setName("Shell Action");
            return(p);
        }
        else if(object instanceof ChannelAction){
            ChannelActionPanel p = new ChannelActionPanel((ChannelAction)object);
            p.setName("Channel Action");
            return(p);
        }
        else if(object instanceof ScriptAction){
            ScriptActionPanel p = new ScriptActionPanel((ScriptAction)object);
            p.setName("Script Action");
            return(p);
        }
        return null;
    }

    @Override
    public void removeItem(Component component) {
        if(component instanceof ShellActionPanel){
            list.remove(((ShellActionPanel)component).getObject());
        }
        else if(component instanceof ChannelActionPanel){
            list.remove(((ChannelActionPanel)component).getObject());
        }
        else if(component instanceof ScriptActionPanel){
            list.remove(((ScriptActionPanel)component).getObject());
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
        if(component instanceof ShellActionPanel){
            return (((ShellActionPanel)component).getObject());
        }
        else if(component instanceof ChannelActionPanel){
            return (((ChannelActionPanel)component).getObject());
        }
        else if(component instanceof ScriptActionPanel){
            return (((ScriptActionPanel)component).getObject());
        }
        return null;
    }
}
