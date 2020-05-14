/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.net.InetAddress;
import java.util.Date;
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
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;

/**
 * @author Scott Phillips
 */

public class SendFeedbackAction extends AbstractAction
{

    /**
     *
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        String page = request.getParameter("page");
        String address = request.getParameter("email");
        String agent = request.getHeader("User-Agent");
        String session = request.getSession().getId();
        String comments = request.getParameter("comments");

        //Check captcha
        String number1 = request.getParameter("number1");
        String number2 = request.getParameter("number2");
        String result = request.getParameter("captcha_input");
        Boolean isValidUser = isValidUser(number1,number2,result);

        // Obtain information from request
        // The page where the user came from
        String fromPage = request.getHeader("Referer");
        // Prevent spammers and splogbots from poisoning the feedback page
        String host = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname");

        String basicHost = "";
        if ("localhost".equals(host) || "127.0.0.1".equals(host)
                        || host.equals(InetAddress.getLocalHost().getHostAddress()))
        {
            basicHost = host;
        }
        else
        {
            // cut off all but the hostname, to cover cases where more than one URL
            // arrives at the installation; e.g. presence or absence of "www"
            int lastDot = host.lastIndexOf('.');
            basicHost = host.substring(host.substring(0, lastDot).lastIndexOf('.'));
        }

        if ((fromPage == null) || ((!fromPage.contains(basicHost)) && (!isValidReferral(fromPage))))
        {
            // N.B. must use old message catalog because Cocoon i18n is only available to transformed pages.
            throw new AuthorizeException(I18nUtil.getMessage("feedback.error.forbidden"));
        }

        // User email from context
        Context context = ContextUtil.obtainContext(objectModel);
        EPerson loggedin = context.getCurrentUser();
        String eperson = null;
        if (loggedin != null)
        {
            eperson = loggedin.getEmail();
        }

        if (page == null || page.equals(""))
        {
            page = fromPage;
        }

        // Check all data is there
        if ((address == null) || address.equals("")
                || (comments == null) || comments.equals("")
                || (number1 == null) || number2 == null
                || (result == null) || !isValidUser)
        {
            // Either the user did not fill out the form or this is the
            // first time they are visiting the page.
            Map<String,String> map = new HashMap<String,String>();
            map.put("page",page);

            if (address == null || address.equals(""))
            {
                map.put("email", eperson);
            }
            else
            {
                map.put("email", address);
            }

            map.put("comments",comments);

            if (!isValidUser && result!=null) {
                map.put("captchaError", "true");
            } else {
                map.put("captchaError", "");
            }

            return map;
        }

        // All data is there, send the email
        Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "feedback"));
        email.addRecipient(DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("feedback.recipient"));

        email.addArgument(new Date()); // Date
        email.addArgument(address);    // Email
        email.addArgument(eperson);    // Logged in as
        email.addArgument(page);       // Referring page
        email.addArgument(agent);      // User agent
        email.addArgument(session);    // Session ID
        email.addArgument(comments);   // The feedback itself

        // Replying to feedback will reply to email on form
        email.setReplyTo(address);

        // May generate MessageExceptions.
        email.send();

        // Finished, allow to pass.
        return null;
    }

    private boolean isValidReferral(String fromPage)
    {
        String[] allowedReferrers = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("mail.allowed.referrers");
        if (allowedReferrers != null && fromPage != null)
        {
            for (String allowedReferrer : allowedReferrers)
            {
                if (fromPage.contains(allowedReferrer))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValidUser(String number1,String number2,String result) {
        boolean isValidUser=false;
        if(number1!=null && number2!=null && result!=null) {
            try {
                if (Integer.parseInt(number1)+Integer.parseInt(number2)==Integer.parseInt(result))
                    isValidUser=true;
            }
            catch (NumberFormatException e) {
                isValidUser=false;
            }
        }
        return isValidUser;
    }
}
