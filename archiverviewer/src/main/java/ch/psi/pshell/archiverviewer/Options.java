
package ch.psi.pshell.archiverviewer;

import ch.psi.pshell.app.Option;

/**
 *
 */
public enum Options implements Option {
     ARCHIVER_VIEWER_HOME,
     MAXSIZE,
     FROM,
     TO,
     RANGE,
     BINS,
     AUTO_RANGE,
     CHANNEL,
     LOCK_ZOOMS, 
     BACKGROUND_COLOR,
     GRID_COLOR,
     LABEL_FONT,
     PLOT_HEIGHT,
     TIMEOUT,
     LOOKUP_WINDOW;  
         
    public static void addHome(){  
        ARCHIVER_VIEWER_HOME.add("ah", "Archiver Viewer default configuration folder", "path");  
    }
    
    public static void addGeneral(){  
        addHome();                        
        BACKGROUND_COLOR.add("bg", "Set default plot background color");
        GRID_COLOR.add("gc", "Set default plot grid color");
        LABEL_FONT.add("tf", "Set font for tick labels");
        PLOT_HEIGHT.add("ph", "Set the minimum plot height");
        LOOKUP_WINDOW.add("lw", "Display the channel lookup window (true by default)");        
    }
    public static void add(){  
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.devices.Options.addArchiver();                   
        ch.psi.pshell.app.Options.FILE.add("f", "Open a configuration file (.arc)", "path");
        ch.psi.pshell.app.Options.CONFIG.add("cf", "JSON configuration string (as in .arc)", "json");        
        MAXSIZE.add("ms", "Set maximum size for unbinned data");
        FROM.add("fr", "Set start of the query range in the format YYYY-MM-DD HH:mm:SS.sss");
        TO.add("to", "Set end of the query range in the format YYYY-MM-DD HH:mm:SS.sss");
        RANGE.add("rg", "Set query range: Last 1min, Last 10min, Last 1h, Last 12h, Last 24h, Last 7d, Yesterday, Today, Last Week, This Week, Last Month, This Month");
        BINS.add("bn", "Set the number of bins (0 for unbinned)");
        AUTO_RANGE.add("ar", "Set auto-range plots: based on the data, and not fixed to the given time range");
        CHANNEL.add("ch", "Add a channel in the format: <NAME>@<BACKEND> or <NAME> (default backend)");
        LOCK_ZOOMS.add("lz", "Set the default state of the zoom lock button");
        TIMEOUT.add("tm", "Set the server timeout (default 15s)");
        addGeneral();
    }    
    
}
