/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import javax.mail.MessagingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogHelper;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class for handling updates to EPersons
 *
 * Recommended filter:  EPerson+Create
 *
 * @author Stuart Lewis
 */
public class EPersonConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger(EPersonConsumer.class);

    protected EPersonService ePersonService
            = EPersonServiceFactory.getInstance().getEPersonService();

    protected ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Initalise the consumer
     *
     * @throws Exception if error
     */
    @Override
    public void initialize()
        throws Exception {

    }

    /**
     * Consume the event
     *
     * @param context The relevant DSpace Context.
     * @param event   Which Event to consume
     * @throws Exception if error
     */
    @Override
    public void consume(Context context, Event event)
        throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();
        UUID id = event.getSubjectID();

        switch (st) {
            // If an EPerson is changed
            case Constants.EPERSON:
                if (et == Event.CREATE) {
                    // Notify of new user registration
                    String notifyRecipient = configurationService.getProperty("registration.notify");
                    EPerson eperson = ePersonService.find(context, id);
                    if (notifyRecipient == null) {
                        notifyRecipient = "";
                    }
                    notifyRecipient = notifyRecipient.trim();

                    if (!notifyRecipient.equals("")) {
                        try {
                            Email adminEmail = Email
                                .getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "registration_notify"));
                            adminEmail.addRecipient(notifyRecipient);

                            adminEmail.addArgument(configurationService.getProperty("dspace.name"));
                            adminEmail.addArgument(configurationService.getProperty("dspace.ui.url"));
                            adminEmail.addArgument(eperson.getFirstName() + " " + eperson.getLastName()); // Name
                            adminEmail.addArgument(eperson.getEmail());
                            adminEmail.addArgument(new Date());

                            adminEmail.setReplyTo(eperson.getEmail());

                            adminEmail.send();

                            log.info(LogHelper.getHeader(context, "registerion_alert", "user="
                                + eperson.getEmail()));
                        } catch (MessagingException me) {
                            log.warn(LogHelper.getHeader(context,
                                                          "error_emailing_administrator", ""), me);
                        }
                    }

                    // If enabled, send a "welcome" message to the new EPerson.
                    if (configurationService.getBooleanProperty("mail.welcome.enabled", false)) {
                        String addressee = eperson.getEmail();
                        if (StringUtils.isNotBlank(addressee)) {
                            log.debug("Sending welcome email to {}", addressee);
                            try {
                                Email message = Email.getEmail(
                                        I18nUtil.getEmailFilename(context.getCurrentLocale(), "welcome"));
                                message.addRecipient(addressee);
                                message.send();
                            } catch (IOException | MessagingException ex) {
                                log.warn("Welcome message not sent to {}:  {}",
                                        addressee, ex.getMessage());
                            }
                        } else {
                            log.warn("Welcome message not sent to EPerson {} because it has no email address.",
                                    eperson.getID().toString());
                        }
                    }
                } else if (et == Event.DELETE) {
                    // TODO: Implement this if required
                }
                break;
            default:
                log.warn("consume() got unrecognized event: " + event.toString());
        }

    }

    /**
     * Handle the end of the event
     *
     * @param ctx The relevant DSpace Context.
     * @throws Exception if error
     */
    @Override
    public void end(Context ctx)
        throws Exception {

    }

    /**
     * Finish the event
     *
     * @param ctx The relevant DSpace Context.
     */
    @Override
    public void finish(Context ctx) {

    }
}
