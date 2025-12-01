package ch.psi.pshell.framework;

import ch.psi.pshell.app.Option;

/**
 *
 */
public enum Options implements Option {    
    PLUGIN,
    PACKAGE,
    TASK,
    PLOT_SERVER,   
    CLI,
    PLOT_ONLY,  
    SERVER,
    SHELL,    
    DETACHED,   
    PERSIST,  
    LOCK_MODE,  
    BARE,    
    GENERIC,  
    DISABLE_SESSIONS,      
    REDIRECT,
    OFFLINE,
    DISABLED,
    QUIET,
    AUTO_CLOSE,  
    VOLATILE,      
    EXTRACT,  
    VERSIONING,     
    NO_BYTECODE,   
    PYTHON_HOME,    
    QUEUES_PATH,
    DEVICE_POOL,  
    PLUGIN_CONFIG ,    
    TASK_CONFIG,
    SETTINGS,
    LOCAL_STARTUP,    
    USER,
    SCRIPT_TYPE,
    DATA_FORMAT, 
    DATA_LAYOUT,  
    DATA_TRUNCATE,
    DISABLE_PRINT,     
    DISABLE_PLOT,  
    INTERP_ARGS,
    STATUSBAR,
    TOOLBAR,
    DETACHED_PLOTS,         
    HIDE_COMPONENT,    
    ;
            
    public static void addBasic(){        
        ch.psi.pshell.app.Options.FILE.add("f", "File to run or open in file in editor", "path");    
        ch.psi.pshell.app.Options.EVAL.add("e", "Evaluate the script statement", "code");                                            
        TASK.add("t", "Start a task using the format script,delay,interval");     
        PLUGIN.add("p", "Load a plugin");     
        PACKAGE.add("m", "Load a package");     
        CLI.add("c", "Start command line interface");
        PLOT_ONLY.add("x", "Start GUI with plots only");
        SERVER.add("v", "Start in server mode");
        ch.psi.pshell.app.Options.HEADLESS.add("y", "Force headless mode");        
        SHELL.add("w", "Start the shell window only");        
        DETACHED.add("d", "Detached mode: Panel plugins are instantiated in private frames");        
        BARE.add("b", "Bare mode: no plugin is loaded at startup");        
        GENERIC.add("g", "Local initialization script is not executed at startup");        
        REDIRECT.add("r", "Redirect standard output to Output window");
        OFFLINE.add("o", "Start in offline mode: data access only");
        DISABLED.add("n", "Interpreter is not started");
        QUIET.add("q", "Quiet mode");        
        AUTO_CLOSE.add("a", "Auto close after executing file");        
        VOLATILE.add("z", "Home folder is volatile (created in tmp folder)");              
    }

    public static void addInterpreter(){   
        USER.add("user", "Set the startup user");        
        SCRIPT_TYPE.add("type", "Set the script type, overriding the setup: py, cpy, jv or groovy");        
        DATA_FORMAT.add("dfmt", "Set the data format, overriding the configuration: h5, txt, csv or fda");        
        DATA_LAYOUT.add("dlay", "Set the data layout, overriding the configuration: default, table, sf or fda");
        DATA_TRUNCATE.add("dtru", "Set if truncate existing data files (default=true)");        
        DISABLE_PLOT.add("dspl", "Disable scan plots");
        DISABLE_PRINT.add("dspr", "Disable printing scans to console");   
        EXTRACT.add("extr", "Force (true) or disable (false) extraction of startup and utility scrips");     
        VERSIONING.add("vers", "Force versioning enabled (true) or disabled (false)");     
        NO_BYTECODE.add("nbcf", "Force disabling (true) or enabling (false) the use of bytecode files");     
        LOCK_MODE.add("lock", "Lock protection for exclusive (not local) mode: none, user (default) or global");  
        ch.psi.pshell.app.Options.LIBRARY_PATH.add("libp", "Add to library path", "path");     
        ch.psi.pshell.app.Options.CLASS_PATH.add("clsp", "Add to class path", "path");     
        INTERP_ARGS.add("args", "Provide arguments to interpreter", "args");                     
    }
    
    public static void addInterface(){   
        STATUSBAR.add("sbar", "Append status bar to detached windows");
        TOOLBAR.add("tbar", "Append tool bar to detached processors");                
        DETACHED_PLOTS.add("dplt", "Create plots for detached windows");  
        HIDE_COMPONENT.add("hcmp", "Hide component in GUI given its name");      
        PERSIST.add("pers", "Persist state of frames");                
        PLOT_SERVER.add("psrv", "URL of a plot server (plots are externalized adding '-dspl -hcmp tabPlots')", "url");     
        DISABLE_SESSIONS.add("diss", "Disable session management");        
    }
    
    public static void addPath(){                         
        ch.psi.pshell.app.Options.addPath();   
        QUEUES_PATH.add("quep", "Set default location for queue files (default is " + QUEUES_PATH.toEnvVar() + " or else {script})", "path");        
        PYTHON_HOME.add("pyhm", "Set CPython home (default is PYTHONHOME)", "path");                                
        DEVICE_POOL.add("pool", "Set devices device pool config file (default is " + DEVICE_POOL.toEnvVar() + ")", "path");
        PLUGIN_CONFIG.add("plug", "Set plugins definition file (default is " + PLUGIN_CONFIG.toEnvVar() + ")", "path"); 
        TASK_CONFIG.add("tskc", "Set task definition file (default is " + TASK_CONFIG.toEnvVar() + ")", "path");
        SETTINGS.add("sets", "Set settings file (default is " + SETTINGS.toEnvVar() + ")", "path");   
        LOCAL_STARTUP.add("loca", "Set local startup script (default is " + LOCAL_STARTUP.toEnvVar() + " or else {script}/" + Setup.DEFAULT_LOCAL_STARTUP_FILE_PREFIX + ")", "path");     
        
        
    }
    
    public static void add(){                
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.devices.Options.addSpecific();        
        addBasic();    
        addPath();        
        addInterpreter();
        addInterface();
    }
        
    
}