package ch.psi.pshell.bs;


/**
 * Configuration for Dispatcher object.
 */
public class DispatcherConfig extends ProviderConfig {

    public boolean disableCompression = false;
    
    public enum Incomplete {
        provide_as_is,
        drop,
        fill_null;

        public String getConfigValue() {
            return this.toString().replace("_", "-");
        }
    }

    public enum Inconsistency {
        drop,
        keep_as_is,
        adjust_individual,
        adjust_global;

        public String getConfigValue() {
            return this.toString().replace("_", "-");
        }
    }

    public enum Strategy {
        complete_all,
        complete_latest;

        public String getConfigValue() {
            return this.toString().replace("_", "-");
        }
    }

    public enum BuildChannelConfig {
        at_startup,
        as_data_arrives;

        public String getConfigValue() {
            return this.toString().replace("_", "-");
        }
    }

    //Send behavior
    public Incomplete mappingIncomplete;
    public Inconsistency validationInconsistency;
    public Strategy sendStrategy;
    public BuildChannelConfig sendBuildChannelConfig;
    public int sendSyncTimeout;
    public boolean sendAwaitFirstMessage;
    
    @Override
    public void updateFields() {
        super.updateFields();
        if (mappingIncomplete==null){
            mappingIncomplete = Incomplete.fill_null;
        }
        if (validationInconsistency==null){
            validationInconsistency = Inconsistency.keep_as_is;
        }
        if (sendStrategy == null){
            sendStrategy = Strategy.complete_all;
        }
        if (sendBuildChannelConfig == null){
            sendBuildChannelConfig = BuildChannelConfig.at_startup;   
        }
    }

}
