
package ch.psi.pshell.screenpanel;

import ch.psi.pshell.app.Option;
import ch.psi.pshell.app.Setup;

/**
 *
 */
 public enum Options implements Option {
    TYPE, 
    LIST, 
    CAM_ALIAS, 
    CAM_NAME, 
    STREAM, 
    CONSOLE, 
    SP_MODE, 
    STREAM_LIST, 
    BUFFER_SIZE, 
    SHARED, 
    SIDEBAR, 
    PERSIST_STATE,
    LOCAL_FIT,
    USER_OVERLAYS;
 
    public static void add(){           
        ch.psi.pshell.app.Options.addBasic();
        ch.psi.pshell.app.Options.HOME_PATH.add("home" ,"Set home folder (default is " + ch.psi.pshell.app.Options.HOME_PATH.toEnvVar() + " or else " + Setup.DEFAULT_HOME_FOLDER +")", "path");
        ch.psi.pshell.app.Options.DATA_PATH.add("data", "Set data folder (default is " + ch.psi.pshell.app.Options.DATA_PATH.toEnvVar() + " or else {home}/data)", "path");
        ch.psi.pshell.app.Options.DEVICES_PATH.add("devp", "Set devices config folder (default is " + ch.psi.pshell.app.Options.DEVICES_PATH.toEnvVar() + " or else {home}/devices)", "path");
        ch.psi.pshell.devices.Options.addEpics();
        ch.psi.pshell.devices.Options.addCamServer();   
        CAM_NAME.add("cam", "Camera name");
        STREAM.add("str", "Strem URL");
        TYPE.add("ty", "Camera type to display");
        LIST.add("li", "Camera list to display");
        STREAM_LIST.add("sl", "Stream list to display");
        CAM_ALIAS.add("al", "Use camera aliases (exclusively if value is 'only')");
        CONSOLE.add("cn", "Accept console commands");
        SP_MODE.add("md", "Operating mode: Cameras(defalut), Pipelines, Instances or Single");        
        BUFFER_SIZE.add("bs", "Image buffer size (default 1)");
        SHARED.add("sh", "If true use shared pipeline instances (default)");
        SIDEBAR.add("sb", "Add sidebar at startup");
        PERSIST_STATE.add("pe", "Persist device state: false, individual (individual per stream), true (default, unique for all streams)");
        LOCAL_FIT.add("lc", "Execute fit calculations localy - stream values not userd");
        USER_OVERLAYS.add("uo", "Set user overlays file name");
    }   
}
