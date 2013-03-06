
package org.dspace.app.xmlui.aspect.dryadfeedback;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

/**
 * Sends a subscription request email to a mailman email list.
 * @author Dan Leehr
 */

public class SubscribeMailingListAction extends AbstractAction
{

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        String subscriber = request.getParameter("email");

        // Check if email is present
        if ((subscriber == null) || subscriber.equals("")) {
            // Either the user did not fill out the form or this is the
            // first time they are visiting the page.
            Map<String,String> map = new HashMap<String,String>();
            return map;
        }
        // Obtain information from request
        // The page where the user came from
        String fromPage = request.getHeader("Referer");

        // Prevent spammers and splogbots from poisoning the feedback page
        String host = ConfigurationManager.getProperty("dspace.hostname");
        String allowedReferrersString = ConfigurationManager.getProperty("mail.allowed.referrers");

        String[] allowedReferrersSplit = null;
        boolean validReferral = false;

        if((allowedReferrersString != null) && (allowedReferrersString.length() > 0))
        {
            allowedReferrersSplit = allowedReferrersString.trim().split("\\s*,\\s*");
            for(int i = 0; i < allowedReferrersSplit.length; i++)
            {
                if(fromPage.indexOf(allowedReferrersSplit[i]) != -1)
                {
                    validReferral = true;
                    break;
                }
            }
        }

        String basicHost = "";
        if (host.equals("localhost") || host.equals("127.0.0.1")
                        || host.equals(InetAddress.getLocalHost().getHostAddress()))
            basicHost = host;
        else
        {
            // cut off all but the hostname, to cover cases where more than one URL
            // arrives at the installation; e.g. presence or absence of "www"
            int lastDot = host.lastIndexOf(".");
            int dotBeforeLast = host.substring(0, lastDot).lastIndexOf(".");

            if(dotBeforeLast < 0)
            {
            basicHost = host;
            }
            else
            {
                basicHost = host.substring(dotBeforeLast);
            }
        }

        if ((fromPage == null) || ((fromPage.indexOf(basicHost) == -1) && (validReferral == false)))
        {
            // N.B. must use old message catalog because Cocoon i18n is only available to transformed pages.
            throw new AuthorizeException("The subscribe page may only be invoked from another DSpace page ");
        }
        Context context = ContextUtil.obtainContext(objectModel);

        // All data is there, send the email
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "subscribe_mailinglist"));
        email.addRecipient(ConfigurationManager.getProperty("subscribe_mailinglist.recipient"));
        email.addArgument(subscriber); // Email address of subscriber

        // May generate MessageExceptions.
        email.send();

        return null;
    }

}