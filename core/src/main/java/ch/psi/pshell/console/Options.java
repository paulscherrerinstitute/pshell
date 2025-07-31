
package ch.psi.pshell.console;

import ch.psi.pshell.app.Option;


/**
 *
 */
 public enum Options implements Option {
    DUMMY;
    
    public static void add(){     
        ch.psi.pshell.framework.Options.add();
        ch.psi.pshell.app.Options.CONFIG.add("cfg", "Set config file (default is " + ch.psi.pshell.app.Options.CONFIG.toEnvVar() + " or else {config}/config.properties)", "path");
    }        
}
