
package ch.psi.pshell.workbench;

import ch.psi.pshell.app.Option;


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
    }    
}
