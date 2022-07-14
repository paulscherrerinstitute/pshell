package ch.psi.pshell.xscan;

import ch.psi.pshell.core.Context;
import ch.psi.pshell.data.LayoutFDA;
import ch.psi.pshell.ui.App;
import ch.psi.utils.EventBus;
import java.util.logging.Logger;

public class AcquisitionConfiguration {
	
	
	private static final Logger logger = Logger.getLogger(AcquisitionConfiguration.class.getName());
		
	private String crlogicPrefix;
	private String crlogicIoc;
	
	/**
	 * Base directory for data. The directory may contain date macros. The string may contain any @see java.text.SimpleDateFormat
	 * patterns within ${ } brackets. The macros are resolved with the actual time while the get method
	 * of this property is called.
	 */
	private String dataBaseDirectory = System.getProperty("user.home");
	/**
	 * Prefix of the data file. The prefix may contain date macros. The string may contain any @see java.text.SimpleDateFormat
	 * patterns within ${ } brackets. The macros are resolved with the actual time while the get method
	 * of this property is called.
	 */
	private String dataFilePrefix = "";
	
	/**
	 * Maximum time for a actor move
	 */
	private Long actorMoveTimeout = 600000l; // 10 Minutes maximum move time 
	
        
        private boolean appendSuffix = true;
        
        
        public static EventBus.Mode eventBusModeAcq = EventBus.Mode.SYNC;
        public static EventBus.Mode eventBusModePlot = EventBus.Mode.ASYNC;
	
	
	/**
	 * Default Constructor
	 * The constructor will read the configuration from the /fda.properties file (resource) located in the classpath.
	 */
	public AcquisitionConfiguration(){               
                //Enforce PShell config insted
                crlogicPrefix = App.hasArgument("crlogic.prefix") ? App.getArgumentValue("crlogic.prefix") : "";
                crlogicIoc = App.hasArgument("crlogic.ioc") ? App.getArgumentValue("crlogic.ioc") : "";;		
                actorMoveTimeout = App.hasArgument("move.timeout") ? Long.valueOf(App.getArgumentValue("move.timeout")): 600000l; // 10 Minutes maximum move time 
                dataBaseDirectory = Context.getInstance().getSetup().getDataPath();
                appendSuffix = App.hasArgument("fdanosuffix") ? false : true;
                dataFilePrefix = "";
	}
        
        public String getDataFileNameDefault(){
            String ret = Context.getInstance().getConfig().dataPath;
            if (Context.getInstance().getConfig().fdaSerialization){                            
                ret = ret.replaceAll("./$", "");
                return  ret + "/" + LayoutFDA.getFilePrefix();
            }
            return ret;
        }
	
	public String getCrlogicPrefix() {
		return crlogicPrefix;
	}

	public void setCrlogicPrefix(String crlogicPrefix) {
		this.crlogicPrefix = crlogicPrefix;
	}
	
	public void setCrlogicIoc(String crlogicIoc) {
		this.crlogicIoc = crlogicIoc;
	}
	public String getCrlogicIoc() {
		return crlogicIoc;
	}
	

	public String getDataBaseDirectory() {
		return dataBaseDirectory;
	}

	public void setDataBaseDirectory(String dataBaseDirectory) {
		this.dataBaseDirectory = dataBaseDirectory;
	}

	public String getDataFilePrefix() {
                if((dataFilePrefix == null) || (dataFilePrefix.trim().length()==0)){
                    return getDataFileNameDefault();
                }
		return dataFilePrefix;
	}

	public void setDataFilePrefix(String dataFilePrefix) {
		this.dataFilePrefix = dataFilePrefix;
	}

	public Long getActorMoveTimeout() {
		return actorMoveTimeout;
	}

	public void setActorMoveTimeout(Long actorMoveTimeout) {
		this.actorMoveTimeout = actorMoveTimeout;
	}
        
        public void setAppendSuffix(boolean value){
            appendSuffix = value;
        }

        public boolean getAppendSuffix(){
            return appendSuffix;
        }
        
}
