package ch.psi.pshell.app;

/**
 *
 */
public enum Options implements Option {
    HELP,       //Only used as command-line argument
    VERSION,    //Only used as command-line argument
    COMMAND,    //Only used as environment variable
    TOKEN,      //Only used as environment variable
    CONFIG,
    FILE,
    EVAL,
    START,
    SIZE,
    LAF,
    UI_SCALE,
    CONSOLE_LOG,    
    LIBRARY_PATH,
    CLASS_PATH,
    HIDE,
    HEADLESS,
    DEBUG,
    FULL_SCREEN,
    QUALITY,
    TITLE,
    LOCAL,
    HOME_PATH,
    OUTPUT_PATH,
    DATA_PATH,
    SCRIPTS_PATH,
    DEVICES_PATH,
    PLUGINS_PATH,
    EXTENSIONS_PATH,
    LOGS_PATH,
    CONTEXT_PATH,
    IMAGES_PATH,
    SESSIONS_PATH,
    CONFIG_PATH,
    WWW_PATH,
    SCRIPT_LIB;

    public static void addBasic(){        
        
        HELP.add("h", "Show help");        
        VERSION.add("vr", "Show version");
        DEBUG.add("db", "Set debug mode");
        LAF.add("lf", "Look and feel: system, metal, nimbus, flat, dark or black");             
        SIZE.add("sz", "Set application window size");     
        FULL_SCREEN.add("fs", "Start in full screen mode");  
        UI_SCALE.add("us", "UI Scale factor as factor (e.g 0.75, 75% or 72dpi)");       
        CONSOLE_LOG.add("cl", "Set the console logging level");        
        QUALITY.add("qt", "Set plot rendering quality: Low, Medium, High, Maximum");        
        TITLE.add("ti", "Application title");                
        START.add("st", "Execute command line command on startup");  
        HIDE.add("hd", "Hide graphical user interface at startup");
        LOCAL.add("l", "Local  mode (multiple instances) : no servers, no lock, no context persistence");                
    }                          
    
    public static void addPath(){                         
        HOME_PATH.add("home" , "Set home folder (default is " + HOME_PATH.toEnvVar() + " or else " + Setup.DEFAULT_HOME_FOLDER +")", "path");
        OUTPUT_PATH.add("outp", "Set output folder (default is " + OUTPUT_PATH.toEnvVar() + " or else " + Setup.DEFAULT_HOME_FOLDER +")", "path");
        SCRIPTS_PATH.add("scpt", "Set script folder (default is " + SCRIPTS_PATH.toEnvVar() + " or else {home}/script)", "path");
        DEVICES_PATH.add("devp", "Set devices config folder (default is " + DEVICES_PATH.toEnvVar() + " or else {home}/devices)", "path");
        PLUGINS_PATH.add("plgp", "Set plugin folder (default is " + PLUGINS_PATH.toEnvVar() + " or else {home}/plugin)", "path");      
        CONFIG_PATH.add("cfgp", "Set config folder (default is " + CONFIG_PATH.toEnvVar() + " or else {home}/config)", "path");    
        EXTENSIONS_PATH.add("extp", "Set extensions folder (default is " + EXTENSIONS_PATH.toEnvVar() + " or else {home}/extensions)", "path");               
        WWW_PATH.add("wwwp", "Set www folder (default is " + WWW_PATH.toEnvVar() + " or else {home}/www)", "path");           
        DATA_PATH.add("data", "Set data folder (default is " + DATA_PATH.toEnvVar() + " or else {outp}/data)", "path");
        IMAGES_PATH.add("imgp", "Set image folder (default is " + IMAGES_PATH.toEnvVar() + " or else {data})", "path"); 
        LOGS_PATH.add("logp", "Set log folder (default is " + LOGS_PATH.toEnvVar() + " or else {outp}/logs)", "path");
        CONTEXT_PATH.add("ctxp", "Set context folder (default is " + CONTEXT_PATH.toEnvVar() + " or else {outp}/context)", "path");    
        SESSIONS_PATH.add("sesp", "Set sessions folder (default is " + SESSIONS_PATH.toEnvVar() + " or else {outp}/sessions)", "path");    
        SCRIPT_LIB.add("sclp", "Script library path (default is " + SCRIPT_LIB.toEnvVar() + " or else " + Setup.DEFAULT_LIB_FOLDER +")", "path");        
    }              
        
}