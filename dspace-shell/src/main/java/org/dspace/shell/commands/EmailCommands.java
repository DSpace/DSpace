/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.commands;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.shell.services.EmailService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * Email commands for the DSpace Spring Shell
 * @author paulo-graca
 *
 */
@ShellComponent(
    value = "email"
)
public class EmailCommands {

    /** 
     * DSpace Configuration service
     */
    ConfigurationService config;

    /** 
     * Email service
     */
    EmailService emailService;

    /**
     * constructor for dependency injection
     */
    public EmailCommands(ConfigurationService config, EmailService emailService) {
        this.config = config;
        this.emailService = emailService;
    }

    /**
     * Send a test email using DSpace mail configuration
     */
    @ShellMethod(
        key = {"email-test"},
        value = "Send a test email using DSpace mail configuration",
        group = "email"
    )
    public void sendTestEmail() {
        ConfigurationService config
                = DSpaceServicesFactory.getInstance().getConfigurationService();

        String to = config.getProperty("mail.admin");
        String subject = "DSpace test email";
        String server = config.getProperty("mail.server");
        String url = config.getProperty("dspace.ui.url");
        String content = "This is a test email sent from DSpace: " + url;

        try {

            System.out.println("\nAbout to send test email...");
            emailService.sendTestEmail(to, subject, content, server);
            System.out.println("Test email sent successfully to: " + to);

        } catch (Exception e) {
            System.out.println("Failed to send email!");
            System.out.println(e.getMessage());
        }

    }

}
