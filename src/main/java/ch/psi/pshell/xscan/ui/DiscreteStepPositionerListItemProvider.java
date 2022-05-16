package ch.psi.pshell.xscan.ui;

import ch.psi.pshell.xscan.model.ArrayPositioner;
import ch.psi.pshell.xscan.model.DiscreteStepPositioner;
import ch.psi.pshell.xscan.model.FunctionPositioner;
import ch.psi.pshell.xscan.model.LinearPositioner;
import ch.psi.pshell.xscan.model.PseudoPositioner;
import ch.psi.pshell.xscan.model.RegionPositioner;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class DiscreteStepPositionerListItemProvider implements ListItemProvider<DiscreteStepPositioner> {

    private List<DiscreteStepPositioner> list;

    private final String[] positioners = new String[]{"Linear Positioner", "Region Positioner", "Array Positioner", "Pseudo Positioner", "Function Positioner"};

    public DiscreteStepPositionerListItemProvider(List<DiscreteStepPositioner> list){
        this.list = list;
    }

    @Override
    public String[] getItemKeys() {
        return(positioners);
    }

    @Override
    public Component newItem(String key) {
        if(key.equals(positioners[0])){
            LinearPositioner lp = new LinearPositioner();
            list.add(lp);
            return(getItem(lp));
        }
        else if(key.equals(positioners[1])){
            RegionPositioner rp = new RegionPositioner();
            list.add(rp);
            return(getItem(rp));
        }
         else if(key.equals(positioners[2])){
            ArrayPositioner ap = new ArrayPositioner();
            list.add(ap);
            return(getItem(ap));
        }
        else if(key.equals(positioners[3])){
            PseudoPositioner pp = new PseudoPositioner();
            list.add(pp);
            return(getItem(pp));
        }
        else if(key.equals(positioners[4])){
            FunctionPositioner pp = new FunctionPositioner();
            list.add(pp);
            return(getItem(pp));
        }
        return null;
    }

     @Override
    public List<Component> getItems() {
        List<Component> l = new ArrayList<Component>();
        for(DiscreteStepPositioner p: list){
            l.add(getItem(p));
        }
        return l;
    }

    private Component getItem(DiscreteStepPositioner object) {
        if(object instanceof LinearPositioner){
            LinearPositionerPanel p = new LinearPositionerPanel((LinearPositioner)object);
            p.setName("Linear P");
            return(p);
        }
        else if(object instanceof RegionPositioner){
            RegionPositionerPanel p = new RegionPositionerPanel((RegionPositioner)object);
            p.setName("Region P");
            return(p);
        }
        else if(object instanceof ArrayPositioner){
            ArrayPositionerPanel p = new ArrayPositionerPanel((ArrayPositioner)object);
            p.setName("Array P");
            return(p);
        }
        else if(object instanceof PseudoPositioner){
            PseudoPositionerPanel p = new PseudoPositionerPanel((PseudoPositioner)object);
            p.setName("Pseudo P");
            return(p);
        }
        else if(object instanceof FunctionPositioner){
            FunctionPositionerPanel p = new FunctionPositionerPanel((FunctionPositioner)object);
            p.setName("Function P");
            return(p);
        }

        return null;
    }

    @Override
    public void removeItem(Component component) {
        DiscreteStepPositioner o = null;

        if(component instanceof LinearPositionerPanel){
            o = ((LinearPositionerPanel)component).getObject();
            list.remove(o);
        }
        else if(component instanceof RegionPositionerPanel){
            o = ((RegionPositionerPanel)component).getObject();
            list.remove(o);
        }
        else if(component instanceof ArrayPositionerPanel){
            o = ((ArrayPositionerPanel)component).getObject();
            list.remove(o);
        }
        else if(component instanceof PseudoPositionerPanel){
            o = ((PseudoPositionerPanel)component).getObject();
            list.remove(o);
        }
        else if(component instanceof FunctionPositionerPanel){
            o = ((FunctionPositionerPanel)component).getObject();
            list.remove(o);
        }

        // Find references to this object and remove the reference
        if(o != null){
            ModelUtil.getInstance().findInMappingAndRemove(o);
            ModelUtil.getInstance().refreshAll();
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
        if(component instanceof LinearPositionerPanel){
            return ((LinearPositionerPanel)component).getObject();
        }
        else if(component instanceof RegionPositionerPanel){
            return ((RegionPositionerPanel)component).getObject();
        }
        else if(component instanceof ArrayPositionerPanel){
            return ((ArrayPositionerPanel)component).getObject();
        }
        else if(component instanceof PseudoPositionerPanel){
            return ((PseudoPositionerPanel)component).getObject();
        }
        else if(component instanceof FunctionPositionerPanel){
            return ((FunctionPositionerPanel)component).getObject();
        }
        return null;
    }
}
