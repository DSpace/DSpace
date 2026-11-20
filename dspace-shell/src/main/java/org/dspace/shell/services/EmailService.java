/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.services;

import java.io.IOException;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.shell.DSpaceShellApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;

/**
 * Service responsible for dealing with emails sending using the DSpace framework.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(DSpaceShellApplication.class);

    /** 
     * DSpace Configuration service
     */
    ConfigurationService config;

    /**
     * Send a test email
     * @param recipient the email address to send to
     * @param subject the email subject
     * @param content the content to be sent
     * @param server the email server address
     * @throws Exception
     */
    public void sendTestEmail(String recipient, String subject, String content, String server) throws Exception {
        config = DSpaceServicesFactory.getInstance().getConfigurationService();

        boolean mailServerDisabled = config.getBooleanProperty("mail.server.disabled", false);

        if (mailServerDisabled) {
            throw new Exception ("\nError sending email:"
                    + " - Error: cannot test email because mail.server.disabled is set to true"
                    + "\nPlease see the DSpace documentation for assistance.\n"
                    + "\n");
        }

        Email message;
        try {
            message = new Email();
            message.setContent("testing", content);
            message.setSubject(subject);
            message.addRecipient(recipient);
            log.debug("\nAbout to send test email:");
            log.debug(" - To: " + recipient);
            log.debug(" - Subject: " + subject);
            log.debug(" - Server: " + server);

            message.send();
        } catch (MessagingException | IOException ex) {
            throw new Exception ("\nError sending email:"
                    + " - Error: " + ex.getMessage()
                    + "\nPlease see the DSpace documentation for assistance.\n"
                    + "\n");
        }

    }

}
