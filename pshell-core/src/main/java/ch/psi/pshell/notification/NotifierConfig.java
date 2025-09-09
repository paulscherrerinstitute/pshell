package ch.psi.pshell.notification;

import ch.psi.pshell.utils.Config;
import ch.psi.pshell.utils.Mail;

/**
 *
 */

public class NotifierConfig  extends Config {
    public String from = "";
    public String to  = "";
    public String host  = "";        
    public Integer port  = 0;
    public String usr = "";
    public String pwd = "";   
    public String smsSuffix = "@sms.switch.ch";
    public Mail.Authentication auth = Mail.Authentication.None;
}
    