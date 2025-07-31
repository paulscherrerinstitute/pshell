package ch.psi.pshell.swing;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 *
 */
public class StandardFrame  extends JFrame{
    static final Logger logger = Logger.getLogger(StandardFrame.class.getName());
    final Timer timer1s;
    
    public StandardFrame() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                //setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                try {
                    onOpen();
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    onClosing();
                } catch (Exception ex) {
                    logger.log(Level.FINE, null, ex);
                }
            }
        });
        addHierarchyListener((HierarchyEvent e) -> {
            if ((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) != 0) {
                if (isShowing()) {
                    try {
                        onShow();
                        StandardFrame.this.timer1s.start();
                    } catch (Exception ex) {
                        logger.log(Level.FINE, null, ex);
                    }
                } else {
                    try {
                        StandardFrame.this.timer1s.stop();
                        onHide();
                    } catch (Exception ex) {
                        logger.log(Level.FINE, null, ex);
                    }
                }
            }
        });
        timer1s = new Timer(1000, (ActionEvent e) -> {
            try {
                onTimer();
            } catch (Exception ex) {
                logger.log(Level.INFO, null, ex);
            }
        });        
    }
    
    @Override
    public void dispose() {
        try {
            onDispose();
        } catch (Exception ex) {
            logger.log(Level.FINE, null, ex);
        }
        super.dispose();
    }

    Boolean visible;

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == null) {
            onCreate();
        }
        this.visible = visible;
        super.setVisible(visible);
    }

        /**
     * Called once when frame is created, before being visible
     */
    protected void onCreate() {
    }

    /**
     * Called once in the first time the frame is shown
     */
    protected void onOpen() {
    }

    /**
     * Called once when the frame is about to be disposed
     */
    protected void onDispose() {
    }

    /**
     * Called every time the frame is shown (also before open is called)
     */
    protected void onShow() {
    }

    /**
     * Called every time the frame is hidden (also before disposed)
     */
    protected void onHide() {
    }

    /**
     * Called every second if the frame is visible
     */
    protected void onTimer() {
    }

//    /**
//     * Called when window is being closed
//     */
    protected void onClosing() {
    }

    
    protected void onLafChange() {
    }    
}
