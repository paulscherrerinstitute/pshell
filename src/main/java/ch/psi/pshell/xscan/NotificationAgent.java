package ch.psi.pshell.xscan;

import ch.psi.pshell.core.Configuration;
import ch.psi.pshell.core.Context;
import ch.psi.pshell.xscan.model.Recipient;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent to send out notifications to specified recipients.
 */
public class NotificationAgent {

    private final List<Recipient> recipients = new ArrayList<Recipient>();

    public void sendNotification(String aSubject, String aBody, boolean error, boolean success) {
        if (Context.getInstance().getConfig().notificationLevel != Configuration.NotificationLevel.Off) {
            for (Recipient recipient : recipients) {

                if ((error && recipient.isError()) || (success && recipient.isSuccess())) {
                    String receiver = recipient.getValue();
                    try {
                        Context.getInstance().getNotificationManager().send(aSubject, aBody, null,new String[]{receiver}) ;
                    } catch (Exception ex) {
                        Logger.getLogger(NotificationAgent.class.getName()).log(Level.WARNING, "Failed to send notification to " + receiver, ex);
                    }
                }
            }
        }
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

}
