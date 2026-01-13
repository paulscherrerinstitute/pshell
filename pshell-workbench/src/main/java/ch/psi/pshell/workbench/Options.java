
package ch.psi.pshell.workbench;

import ch.psi.pshell.app.Option;
import java.lang.reflect.Method;
import java.util.ArrayList;


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
        ch.psi.pshell.stripchart.Options.addHome();
        ch.psi.pshell.archiverviewer.Options.addHome();
        
        String options = System.getenv().get("PSHELL_EX_OPTIONS");
        if ((options!=null) && (!options.isBlank())){
            var additionalOptions = new ArrayList<String>();
            for (String op: options.split("\\|")){                
                if (!op.isBlank()){
                    op = op.trim();
                    try{
                        Class cls = Class.forName(op);
                        Method add = cls.getMethod("add");
                        add.invoke(null);
                    } catch (Exception ex){
                        additionalOptions.add(op);
                    }
                }
            }
            if (!additionalOptions.isEmpty()){
                var sb = new StringBuilder();
                sb.append("Additional Options:\n");
                for (String option: additionalOptions){
                    sb.append(" ");
                    sb.append(option);
                    sb.append("\n");
                }
                App.setHelpFooter(sb.toString());
            }
        }
        
    }    
}
