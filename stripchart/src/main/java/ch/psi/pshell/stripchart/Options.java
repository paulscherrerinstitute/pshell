
package ch.psi.pshell.stripchart;

import ch.psi.pshell.app.Option;

/**
 *
 */
public enum Options implements Option {
    STRIPCHART_HOME,
    ATTACH,
    BACKGROUND_COLOR,
    GRID_COLOR,
    LABEL_FONT,
    DEFAULT_DURATION,
    ALARM_INTERVAL,
    ALARM_FILE;
    
    public static void add(){           
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.devices.Options.addEpics();
        ch.psi.pshell.devices.Options.addCamServer();
        ch.psi.pshell.devices.Options.PARALLEL.add("u", "Parallel initialization of devices (values: true or false)");                                
        ch.psi.pshell.app.Options.FILE.add("f", "Open a StripChart configuration file (.scd)", "path");
        ch.psi.pshell.app.Options.CONFIG.add("cf", "JSON configuration string (as in .scd file) or list of channel names", "json");
        ch.psi.pshell.framework.Options.SERVER.add("v", "Create a StripChart server");
        ATTACH.add("at", "Shared mode: try connecting to existing server, or create one if not available");
        BACKGROUND_COLOR.add("bg", "Set default plot background color");
        GRID_COLOR.add("gc", "Set default plot grid color");
        LABEL_FONT.add("tf", "Set font for time plot tick labels");
        DEFAULT_DURATION.add("dd", "Set the plots defaut duration (default 60000ms)");
        ALARM_INTERVAL.add("ai", "Set the alarm timer interval (default 1000ms)");
        ALARM_FILE.add("af", "Set alarm sound file (default use system beep)");
        STRIPCHART_HOME.add("sh", "StripChart default configuration folder", "path");  
    }
    
}
