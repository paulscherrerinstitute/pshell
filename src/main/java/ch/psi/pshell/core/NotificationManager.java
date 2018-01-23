/*
 * Copyright (c) 2014 Paul Scherrer Institute. All rights reserved.
 */

package ch.psi.pshell.core;

import ch.psi.utils.Arr;
import ch.psi.utils.Config;
import ch.psi.utils.Mail;
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
        try {
            config.load(getMailConfigFile().toString());
        } catch (IOException ex) {
            Logger.getLogger(NotificationManager.class.getName()).log(Level.WARNING, null, ex);
        }        
        
        if ((config.to!=null) && (!config.to.trim().isEmpty())){
            to = config.to.split(";");
            to = validateRecipients(to);
        } 
        
        from = ((config.from==null)||(config.from.isEmpty()))? Sys.getProcessName() : config.from;
        
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

    public boolean isEnabled(){
        return (config.host!=null);
    }

    public void assertEnabled() throws IOException{
        if (!isEnabled()){
            throw new IOException("Mailing is not configured");
        }
    }
    
    @Override
    public void close() throws Exception {
        config.close();
    }
    
}
