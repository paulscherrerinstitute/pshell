package ch.psi.pshell.plotter;

import ch.psi.pshell.app.Setup;
import java.awt.Dimension;


/**
 *
 */
public class App extends ch.psi.pshell.app.App{
        
    public static int getPort(){
        return Options.PORT.getInt(-1);
    }
    
    public static void create(){
        boolean persist = !Setup.isLocal();
        boolean debug =Setup.isDebug();
        
        create(getPort(), Setup.getSize(), persist, debug, null);
    }
    
    public static void create(int port, Dimension size, boolean persisted, boolean debug, String title){
        String dialogTitle = (title==null) ? Setup.getTitle() : title;
        PlotServer.debug = debug;
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               View view = new View(port, persisted, dialogTitle);
               if (size!=null){
                   view.setSize(size);
               }
               view.setVisible(true);
            }
        });        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        Options.add();
        App.init(args);                
        create();
    }    
}
