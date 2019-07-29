package ch.psi.pshell.core;

import ch.psi.pshell.device.GenericDevice;
import ch.psi.utils.Config;

/**
 * Interface for receiving user interaction events from the scripts.
 */
public interface UserInterface {

    public String getString(String message, String defaultValue) throws InterruptedException;

    public String getString(String message, String defaultValue, String[] alternatives) throws InterruptedException;

    public String getPassword(String msg, String title) throws InterruptedException;

    /**
     *
     * @param message
     * @param type 'YesNo','YesNoCancel' or 'OkCancel'
     * @return 'Yes', 'No' or 'Cancel'
     */
    public String getOption(String message, String type) throws InterruptedException;

    public void showMessage(String message, String title, boolean blocking) throws InterruptedException;

    public Object showPanel(GenericDevice dev) throws InterruptedException;
    
    public Object showPanel(Config config) throws InterruptedException;
    
    public int waitKey(int timeout) throws InterruptedException;
}
