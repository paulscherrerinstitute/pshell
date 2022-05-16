package ch.psi.pshell.xscan.ui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ListUtil {

    public static void moveItemUp(List list, Object object) {
        Logger.getLogger(ListUtil.class.getName()).log(Level.INFO, "Move item up"+object);
        Integer index = null;
        for(int i=0;i<list.size();i++){
            if(list.get(i).equals(object)){
                index = i;
                break;
            }
        }

        if(index!=null && index>0){
            Object a = list.remove((int)index);
            list.add(index-1, a);
        }
    }

    public static void moveItemDown(List list, Object object) {
        Logger.getLogger(ListUtil.class.getName()).log(Level.INFO, "Move item down"+object);
        Integer index = null;
        for(int i=0;i<list.size();i++){
            if(list.get(i).equals(object)){
                index = i;
                break;
            }
        }

        if(index!=null && index<(list.size()-1)){
            Object a = list.remove((int)index);
            list.add(index+1, a);
        }
    }
}
