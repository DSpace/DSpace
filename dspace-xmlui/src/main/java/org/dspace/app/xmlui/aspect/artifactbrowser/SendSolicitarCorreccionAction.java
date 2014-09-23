/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;

/**
 * @author Scott Phillips
 * @author Adán Román Ruiz at arvo.es
 */

public class SendSolicitarCorreccionAction extends AbstractAction
{

    /**
     *
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        String page = request.getParameter("page");
       // String address = request.getParameter("email");
        String agent = request.getHeader("User-Agent");
        String session = request.getSession().getId();
        String comments = request.getParameter("comments");
        String handle = parameters.getParameter("handle");
        
        // Obtain information from request
        // The page where the user came from
        String fromPage = request.getHeader("Referer");

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
        if ( (comments == null) || comments.equals("") 
                || (handle == null) || handle.equals(""))
        {
            // Either the user did not fill out the form or this is the
            // first time they are visiting the page.
            Map<String,String> map = new HashMap<String,String>();
            map.put("page",page);
            map.put("comments",comments);
            map.put("handle",handle);
            
            return map;
        }

        // All data is there, send the email
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "solicitarCorreccion"));
        email.addRecipient(ConfigurationManager.getProperty("solicitarCorreccion.recipient"));

        email.addArgument(new Date()); // Date
        email.addArgument(eperson);    // Logged in as
        email.addArgument(page.replace("/solicitarCorreccion/", "/handle/"));       // Referring page
        email.addArgument(agent);      // User agent
        email.addArgument(session);    // Session ID
        email.addArgument(comments);   // The feedback itself

        // Replying to feedback will reply to email on form
        email.setReplyTo(eperson);

        // May generate MessageExceptions.
        email.send();

        // Finished, allow to pass.
        return null;
    }

}
