package uk.ac.edina.datashare.eperson;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonConsumer;
import org.dspace.event.Event;

/**
 * Capture EPerson events to send an email to the user when an account is created.
 */
public class UserEventHandler extends EPersonConsumer
{
    /*
     * (non-Javadoc)
     * @see org.dspace.eperson.EPersonConsumer#consume(org.dspace.core.Context, org.dspace.event.Event)
     */
    public void consume(Context context, Event event)
        throws Exception
    {
        if(event.getSubjectType() == Constants.EPERSON &&
                event.getEventType() == Event.CREATE)
        {
            EPerson eperson = EPerson.find(context, event.getSubjectID());
            Email adminEmail = Email.getEmail(
                    I18nUtil.getEmailFilename(context.getCurrentLocale(), "welcome_message"));
            
            adminEmail.addRecipient(eperson.getEmail());
            adminEmail.addArgument(eperson.getFirstName());
            adminEmail.setReplyTo(ConfigurationManager.getProperty("mail.admin"));
            
            adminEmail.send();
        }
        
        // pass event to base class
        super.consume(context, event);
    }
}
