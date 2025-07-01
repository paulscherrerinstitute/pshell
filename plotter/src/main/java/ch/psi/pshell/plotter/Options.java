
package ch.psi.pshell.plotter;

import ch.psi.pshell.app.Option;

/**
 *
 */
 public enum Options implements Option {
    PORT;

    public static void add(){              
        ch.psi.pshell.app.Options.addBasic();
        PORT.add("pt", "Application TCP port", "int");                
    }    
        
}
