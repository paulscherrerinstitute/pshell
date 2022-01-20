package ch.psi.pshell.core;

import ch.psi.pshell.ui.App;
import ch.psi.utils.Arr;
import ch.psi.utils.Config;
import ch.psi.utils.Mail;
import ch.psi.utils.Str;
import ch.psi.utils.Sys;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class NotificationManager implements AutoCloseable{
    String[] to;
    String from;
    
    public class MailConfig extends Config {
        public String from = "";
        public String to  = "";
        public String host  = "";        
        public Integer port  = 0;
        public String usr = "";
        public String pwd = "";   
        public String smsSuffix = "@sms.switch.ch";
        public Mail.Authentication auth = Mail.Authentication.None;
    }
    
    final MailConfig config;
    
    public Path getMailConfigFile() {
        return Paths.get(Context.getInstance().getSetup().getConfigPath(), "mail.properties");
    }
    
    public NotificationManager(){
        config = new MailConfig();
        initialize();
    }

    public void initialize(){
        try {
            config.load(getMailConfigFile().toString());
        } catch (IOException ex) {
            if (!App.isVolatile()){
                Logger.getLogger(NotificationManager.class.getName()).log(Level.WARNING, null, ex);
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

    public MailConfig getConfig(){
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
                Logger.getLogger(NotificationManager.class.getName()).log(Level.WARNING, "Invalid email address: " + to[i]);
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
