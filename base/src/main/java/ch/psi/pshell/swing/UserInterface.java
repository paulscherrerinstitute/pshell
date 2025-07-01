package ch.psi.pshell.swing;

import ch.psi.pshell.data.DataAddress;
import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Nameable;

/**
 * Interface for receiving user interaction events from the scripts.
 */
public interface UserInterface {

    default public String getString(String message, String defaultValue) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    default public String getString(String message, String defaultValue, String[] alternatives) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    default public String getPassword(String msg, String title) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    /**
     *
     * @param message
     * @param type 'YesNo','YesNoCancel' or 'OkCancel'
     * @return 'Yes', 'No' or 'Cancel'
     */
    default public String getOption(String message, String type) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    default public void showMessage(String message, String title, boolean blocking) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    default public ConfigDialog showConfig(Config config) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }
    
    default public MonitoredPanel showPanel(Nameable dev) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }    
    
    default public MonitoredPanel showPanel(DataAddress path, String message) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }
    
    default public MonitoredPanel showPanel(String text, String title) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }

    default public int waitKey(int timeout) throws InterruptedException{
        throw new java.lang.UnsupportedOperationException();
    }
}