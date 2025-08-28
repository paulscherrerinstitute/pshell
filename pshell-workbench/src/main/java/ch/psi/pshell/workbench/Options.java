
package ch.psi.pshell.workbench;

import ch.psi.pshell.app.Option;
import java.lang.reflect.Method;


/**
 *
 */
 public enum Options implements Option {
    PREFERENCES,
    STDIO; 
    
    public static void add(){           
        ch.psi.pshell.console.Options.add();
        ch.psi.pshell.xscan.Options.addSpecific();
        STDIO.add("dual", "Start GUI command line interface (not allowed if running in the background)");                        
        PREFERENCES.add("pref", "Override the view preferences file", "path");                
        
        String options = System.getenv().get("PSHELL_EX_OPTIONS");
        if ((options!=null) && (!options.isBlank())){
            for (String op: options.split(",")){
                try{
                    Class cls = Class.forName(op.trim());
                    Method add = cls.getMethod("add");
                    add.invoke(null);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
        
    }    
}
