package ch.psi.pshell.security;

import ch.psi.utils.Config;

/**
 *
 */
public class Rights extends Config {

    public boolean denyConfig;
    public boolean denyDeviceConfig;
    public boolean denyDeviceWrite;
    public boolean denyPrefs;
    public boolean denyEdit;
    public boolean denyRun;
    public boolean denyConsole;
    public boolean denyVersioning;

    public boolean hideConsole;
    public boolean hideData;
    public boolean hideDevices;
    public boolean hideScripts;
    public boolean hideEditor;
    public boolean hideToolbar;

    public void assertConfigAllowed() {
        if (denyConfig) {
            throw new UserAccessException("system configuration");
        }
    }

    public void assertPrefsAllowed() {
        if (denyPrefs) {
            throw new UserAccessException("interface setup");
        }
    }

    public void assertDeviceConfigAllowed() {
        if (denyDeviceConfig) {
            throw new UserAccessException("device configuration");
        }
    }

    public void assertDeviceWriteAllowed() {
        if (denyDeviceWrite) {
            throw new UserAccessException("device write commands");
        }
    }

    public void assertRunAllowed() {
        if (denyRun) {
            throw new UserAccessException("script execution");
        }
    }

    public void assertConsoleAllowed() {
        if (denyConsole) {
            throw new UserAccessException("console commands");
        }
    }

    public void assertVersioningAllowed() {
        if (denyVersioning) {
            throw new UserAccessException("versioning control");
        }
    }
}
