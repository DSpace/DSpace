/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.log4j.Logger;
import org.dspace.core.*;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import javax.mail.MessagingException;
import java.util.Date;
import java.util.UUID;

/**
 * Class for handling updates to EPersons
 *
 * Recommended filter:  EPerson+Create
 *
 * @version $Revision$
 *
 * @author Stuart Lewis
 */
public class EPersonConsumer implements Consumer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(EPersonConsumer.class);

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Initalise the consumer
     *
     * @throws Exception if error
     */
    @Override
    public void initialize()
        throws Exception
    {

    }

    /**
     * Consume the event
     *
     * @param context
     * @param event
     * @throws Exception if error
     */
    @Override
    public void consume(Context context, Event event)
        throws Exception
    {
        int st = event.getSubjectType();
        int et = event.getEventType();
        UUID id = event.getSubjectID();

        switch (st)
        {
            // If an EPerson is changed
            case Constants.EPERSON:
                if (et == Event.CREATE)
                {
                    // Notify of new user registration
                    String notifyRecipient = ConfigurationManager.getProperty("registration.notify");
                    if (notifyRecipient == null) {
                        notifyRecipient = "";
                    }
                    notifyRecipient = notifyRecipient.trim();

                    if(!notifyRecipient.equals(""))
                    {
                        try
                        {
                            EPerson eperson = ePersonService.find(context, id);
                            Email adminEmail = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "registration_notify"));
                            adminEmail.addRecipient(notifyRecipient);

                            adminEmail.addArgument(ConfigurationManager.getProperty("dspace.name"));
                            adminEmail.addArgument(ConfigurationManager.getProperty("dspace.url"));
                            adminEmail.addArgument(eperson.getFirstName() + " " + eperson.getLastName()); // Name
                            adminEmail.addArgument(eperson.getEmail());
                            adminEmail.addArgument(new Date());

                            adminEmail.setReplyTo(eperson.getEmail());

                            adminEmail.send();

                            log.info(LogManager.getHeader(context, "registerion_alert", "user="
                                    + eperson.getEmail()));
                        }
                        catch (MessagingException me)
                        {
                            log.warn(LogManager.getHeader(context,
                                "error_emailing_administrator", ""), me);
                        }
                    }
                } else if (et == Event.DELETE)
                {
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
     * @param ctx
     * @throws Exception if error
     */
    @Override
    public void end(Context ctx)
        throws Exception
    {

    }

    /**
     * Finish the event
     *
     * @param ctx
     */
    @Override
    public void finish(Context ctx)
    {

    }
}
