
package ch.psi.pshell.ui;

import ch.psi.pshell.core.Configuration.LogLevel;
import ch.psi.pshell.core.Setup;
import ch.psi.utils.Config;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//Set user defaults for home folder & other command line options in properties file, if not specified in arguments
public class UserOptions extends Config {
    private static final String FILENAME = Setup.expand("~/.pshell.properties");
    
    public UserOptions(){
        super();
        setFileName(FILENAME);
        setPersistNulls(false);
        //Does not create file if does not exist already.
        if (new File(FILENAME).exists()) {
            try {
                load(FILENAME);
            } catch (IOException ex) {
                ex.printStackTrace();
                Logger.getLogger(UserOptions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }   
    }
    
    public String home;
    public String outp;
    public String scpt;
    public String data;
    public String user;
    public String setp;
    public String conf;
    public String plug;
    public String task;
    public String pool;
    public String devp;
    public String plgp;
    public String sesp;
    public String ctxp;
    public String extp;
    public String imgp;
    public String pyhm;
    public String logp;
    public String sets;
    public String pini;
    public String laf;
    public LogLevel clog;
    
}
