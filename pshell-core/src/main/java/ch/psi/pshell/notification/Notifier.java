package ch.psi.pshell.notification;

import ch.psi.pshell.framework.Setup;
import ch.psi.pshell.utils.Arr;
import ch.psi.pshell.utils.Mail;
import ch.psi.pshell.utils.Str;
import ch.psi.pshell.utils.Sys;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class Notifier implements AutoCloseable{
    public static  Notifier INSTANCE;    
    
    public static Notifier getInstance(){
        if (INSTANCE == null){
            throw new RuntimeException("Notifier not instantiated.");
        }        
        return INSTANCE;
    }
    
    public static boolean hasInstance(){
        return INSTANCE!=null;
    }    
        
    
    public enum NotificationLevel {
        Off,
        User,
        Error,
        Completion
    }       
    
    String[] to;
    String from;
   
    
    NotifierConfig config;
    
    public Path getMailConfigFile() {
        return Paths.get(Setup.getConfigPath(), "mail.properties");
    }    
    
    public Notifier(){
        config = new NotifierConfig();
        INSTANCE  = this;        
    }

    public void initialize(){
        try {
            config.load(getMailConfigFile().toString());
        } catch (IOException ex) {
            if (!Setup.isVolatile()){
                Logger.getLogger(Notifier.class.getName()).log(Level.WARNING, null, ex);
            }
        }

        if ((config.to!=null) && (!config.to.trim().isEmpty())){            
            to = validateRecipients(config.to);
        }

        from = ((config.from==null)||(config.from.isEmpty()))? Sys.getProcessName() : config.from;
    }
    
    public String[] getRecipients(){
        return to;
    }

    public void setRecipients(String to) throws IOException{
        setRecipients(validateRecipients(to));
    }
    
    public void setRecipients(String[] to) throws IOException{
        to = validateRecipients(to);
        this.to = to;
        config.to = (to == null) ? "" : String.join(";", to);
        config.save();
    }

    public NotifierConfig getConfig(){
        return config;
    }

    public boolean isEnabled(){
        return (config.host!=null);
    }

    public void assertEnabled() throws IOException{
        if (!isEnabled()){
            throw new IOException("Mailing is not configured");
        }
    }

    private String[] validateRecipients(String to){
        if ((to==null) || (to.isBlank())){
            return null;
        }
        String[] ret = Str.trim(Str.split(to, new String[]{"|", ";", ","}));        
        ret = validateRecipients(ret);
        return ret;
    }
    
    private String[] validateRecipients(String[] to){
        for (int i=0; i< to.length; i++){
            to[i] =to[i].trim();
            if(to[i].matches("[0-9,\\\\.,-,a-z,A-Z]*@[0-9,\\\\.,a-z,A-Z]*")){
                //Valid email
            }
            else if(to[i].matches("[0-9]+")){
                //SMS number
                to[i] = to[i] + config.smsSuffix;
            }
            else{
                Logger.getLogger(Notifier.class.getName()).log(Level.WARNING, "Invalid email address: " + to[i]);
                to[i] = null;
            }                
        }
        to = Arr.removeNulls(to);
        if (to.length==0){
            to=null;
        }
        return to;
    }

    
    public void send(String subject, String text) throws IOException {    
        send(subject, text, (File[])null);
    }
    
    /**
     * Send to configured recipients
     */
    public void send(String subject, String text, File[] attachments) throws IOException {    
        assertEnabled();
        if (config.to==null){
            throw new IOException("Recipients not configured");
        }
        try{
            Mail.send(subject, text, from, to, null, null, attachments, config.host, 
                    config.port<=0 ? null : config.port, config.auth, config.usr, config.pwd);
        } catch (IOException ex){
            throw ex;
        } catch (Exception ex){

            throw new IOException(ex.getMessage());
        }
    }
    
    /**
     * Send to specific recipients
     */
    public void send(String subject, String text, File[] attachments, String[] recipients) throws IOException {    
        assertEnabled();
        String[] to = validateRecipients(recipients);
        try{
            Mail.send(subject, text, from, to, null, null, attachments, config.host, 
                    config.port<=0 ? null : config.port, config.auth, config.usr, config.pwd);
        } catch (IOException ex){
            throw ex;
        } catch (Exception ex){
            throw new IOException(ex.getMessage());
        }
    }
    
    @Override
    public void close() throws Exception {
        config.close();
    }    
}
