package ch.psi.pshell.framework;

import ch.psi.pshell.swing.SwingUtils;
import ch.psi.pshell.utils.Str;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Abstraction for a background activity in the Workbench, extending
 * SwingWorker.
 */
public abstract class Task extends SwingWorker<Object, Void> {

    protected Task() {
        if (Context.getView().getStatusBar() != null) {
            addPropertyChangeListener(Context.getView().getStatusBar());
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    String propertyName = e.getPropertyName();
                    if ("state".equals(propertyName)) {
                        StateValue state = (StateValue) (e.getNewValue());
                        switch (state) {
                            case STARTED:
                                startTimestamp = System.currentTimeMillis();
                                if (trackTime) {
                                    firePropertyChange("timer", -1L, 0L);
                                    if (timerTask != null) {
                                        timerTask.stop();
                                    }
                                    timerTask = new Timer(1000, (ActionEvent ae) -> {
                                        firePropertyChange("timer", -1L, getExecutionTime());
                                    });
                                    timerTask.start();
                                }
                                break;
                            case DONE:
                                endTimestamp = System.currentTimeMillis();
                                if (trackTime) {
                                    firePropertyChange("timer", -1L, -1L);
                                    if (timerTask != null) {
                                        timerTask.stop();
                                        timerTask = null;
                                    }
                                }
                                break;
                        }
                    }
                }
            });

        }
    }

    @Override
    public String toString() {
        return Str.toTitleCase(this.getClass().getSimpleName());
    }
    String message = "";
    boolean trackTime = true;
    
    protected void setTrackTime(boolean trackTime){
        this.trackTime = trackTime;
    }

    protected void setMessage(String message) {

        String oldMessage, newMessage;
        synchronized (this) {
            oldMessage = this.message;
            this.message = message;
            newMessage = this.message;
        }
        firePropertyChange("message", oldMessage, newMessage);
    }

    protected void setCommand(String command) {
        firePropertyChange("command", null, command);
    }

    public String getMessage() {
        return message;
    }

    Timer timerTask;

    volatile long startTimestamp;
    volatile long endTimestamp;

    public long getStartTime() {
        if (getState() != StateValue.PENDING) {
            long timespan = System.currentTimeMillis() - startTimestamp;
            return System.currentTimeMillis() - timespan;
        }
        return -1;
    }

    public long getExecutionTime() {
        if (getState() == StateValue.STARTED) {
            return System.currentTimeMillis() - startTimestamp;
        } else if (endTimestamp != 0) {
            return endTimestamp - startTimestamp;
        } else {
            return -1;
        }
    }       
    
    
    protected void checkShowReturn(Object value){
        if (Context.getView().getScriptPopupMode() == MainFrame.ScriptPopupMode.Return) {
            if ((!Context.isAborted()) && (value != null)) {
                SwingUtils.showMessage(Context.getView(), "Script Return", String.valueOf(value));
            }
        }        
    }
    
    protected void checkShowException(Exception ex){
        if (Context.getView().getScriptPopupMode() != MainFrame.ScriptPopupMode.None) {
            if (!Context.isAborted()) {
                SwingUtils.showMessage(Context.getView(), "Script Error", ex.getMessage(), -1, JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    protected void sendErrorApp(Exception ex){
        Context.getApp().sendError(ex.toString());
    }
}
