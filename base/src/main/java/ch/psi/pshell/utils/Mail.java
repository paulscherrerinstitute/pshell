package ch.psi.pshell.utils;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 */
public class Mail {
    
    public enum Authentication{
        None,
        SSL,
        TLS
    }

    public static void send(String subject, String text, String from, String[] to, String[] cc, String[] bcc, 
            File[] attachments, String host, Integer port, Authentication auth, String usr, String pwd) throws Exception {
        if (auth==null){
            auth = Authentication.None;
        }

        // Get system properties
        Properties properties = System.getProperties();
        // Setup mail server
        properties.put("mail.smtp.host", host);
        if (port != null) {
            properties.put("mail.smtp.port", String.valueOf(port));
        }
        properties.put("mail.transport.protocol", "smtp");
                
        Authenticator authenticator = null;
        if (auth!=Authentication.None) {
            properties.put("mail.smtp.auth", "true");
            authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(usr, pwd);
                }
            };
            if (auth == Authentication.SSL) {
                properties.put("mail.smtp.ssl.enable", "true");                                
                //properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");                                
                //properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
		//properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            } else {
                properties.put("mail.smtp.starttls.enable", "true");                
            }            
        }
        Session session = Session.getInstance(properties, authenticator);
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        if (to != null) {
            for (String str : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(str));
            }
        }
        if (cc != null) {
            for (String str : cc) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(str));
            }
        }   
        if (bcc != null) {
            for (String str : bcc) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(str));
            }
        }
        message.setSubject(subject);
        message.setSentDate(new Date());

        if (attachments == null) {
            message.setText(text);
        } else {
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(text);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            for (File attachment : attachments) {
                messageBodyPart = new MimeBodyPart();
                String filename = attachment.getAbsolutePath();
                DataSource source = new FileDataSource(attachment.getAbsolutePath());
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(attachment.getName());
                multipart.addBodyPart(messageBodyPart);
            }
            message.setContent(multipart);
        }

        Transport.send(message);        
    }
    
}
