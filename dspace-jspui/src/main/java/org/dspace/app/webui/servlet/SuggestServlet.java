/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.MissingResourceException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;


/**
 * Servlet for handling user email recommendations
 *
 * @author  Arnaldo Dantas
 * @version $Revision$
 */
public class SuggestServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(SuggestServlet.class);
    
    private final transient HandleService handleService
            = HandleServiceFactory.getInstance().getHandleService();

    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
    					   HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Obtain information from request
        // The page where the user came from
        String fromPage = request.getHeader("Referer");

        // Prevent spammers and splogbots from poisoning the feedback page
        String host = ConfigurationManager.getProperty("dspace.hostname");

        String basicHost = "";
        if (host.equals("localhost") || host.equals("127.0.0.1")
                || host.equals(InetAddress.getLocalHost().getHostAddress()))
        {
            basicHost = host;
        }
        else
        {
            // cut off all but the hostname, to cover cases where more than one URL
            // arrives at the installation; e.g. presence or absence of "www"
            int lastDot = host.lastIndexOf('.');
            basicHost = host.substring(host.substring(0, lastDot).lastIndexOf("."));
        }

        if (fromPage == null || fromPage.indexOf(basicHost) == -1)
        {
            throw new AuthorizeException();
        }

        // Obtain information from request
        String handle = request.getParameter("handle");

        // Lookup Item title & collection
        String title = null;
        String collName = null;
        if (handle != null && !handle.equals(""))
        {
            Item item = (Item) handleService.resolveToObject(context, handle);
            if (item != null)
            {
                title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
                List<Collection> colls = item.getCollections();
                collName = colls.get(0).getName();
            }
        }
        else
        {
        	String path = request.getPathInfo();
            log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
            JSPManager.showInvalidIDError(request, response, path, -1);
            return;
        }
        if (title == null)
        {
        	title = "";
        }
        if(collName == null)
        {
        	collName = "";
        }
        request.setAttribute("suggest.title", title);

        // User email from context
        EPerson currentUser = context.getCurrentUser();
        String authEmail = null;
        String userName = null;

        if (currentUser != null)
        {
            authEmail = currentUser.getEmail();
            userName = currentUser.getFullName();
        }

        if (request.getParameter("submit") != null)
        {
        	String recipAddr = request.getParameter("recip_email");
            // the only required field is recipient email address
            if (recipAddr == null || recipAddr.equals(""))
            {
                log.info(LogManager.getHeader(context, "show_suggest_form",
                    	 "problem=true"));
                request.setAttribute("suggest.problem", Boolean.TRUE);
                JSPManager.showJSP(request, response, "/suggest/suggest.jsp");
                return;
            }
        	String recipName = request.getParameter("recip_name");
        	if (recipName == null || "".equals(recipName))
        	{
                try
                {
                recipName = I18nUtil.getMessage("org.dspace.app.webui.servlet.SuggestServlet.recipient", context);
                }
                catch (MissingResourceException e)
                {
                    log.warn(LogManager.getHeader(context, "show_suggest_form", "Missing Resource: org.dspace.app.webui.servlet.SuggestServlet.sender"));
                    recipName = "colleague";
                }
        	}
            String senderName = request.getParameter("sender_name");
            if (senderName == null || "".equals(senderName) )
            {
            	// use userName if available
            	if (userName != null)
            	{
            		senderName = userName;
            	}
            	else
            	{
            		try
                    {
            		senderName = I18nUtil.getMessage("org.dspace.app.webui.servlet.SuggestServlet.sender", context);
                    }
                    catch (MissingResourceException e)
                    {
                        log.warn(LogManager.getHeader(context, "show_suggest_form", "Missing Resource: org.dspace.app.webui.servlet.SuggestServlet.sender"));
                        senderName = "A DSpace User";
                    }
            	}
            }
            String senderAddr = request.getParameter("sender_email");
            if (StringUtils.isEmpty(senderAddr) && authEmail != null)
            {
            	// use authEmail if available
                senderAddr = authEmail;
            }
            String itemUri = handleService.getCanonicalForm(handle);
            String itemUrl = handleService.resolveToURL(context, handle);
            String message = request.getParameter("message");
            String siteName = ConfigurationManager.getProperty("dspace.name");

            // All data is there, send the email
            try
            {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "suggest"));
                email.addRecipient(recipAddr);	 // recipient address
                email.addArgument(recipName);    // 1st arg - recipient name
                email.addArgument(senderName);   // 2nd arg - sender name
                email.addArgument(siteName);     // 3rd arg - repository name
                email.addArgument(title);        // 4th arg - item title
                email.addArgument(itemUri);      // 5th arg - item handle URI
                email.addArgument(itemUrl);      // 6th arg - item local URL
                email.addArgument(collName);     // 7th arg - collection name
                email.addArgument(message);      // 8th arg - user comments

                // Set sender's address as 'reply-to' address if supplied
                if ( senderAddr != null && ! "".equals(senderAddr))
                {
                	email.setReplyTo(senderAddr);
                }

                // Only actually send the email if feature is enabled
                if (ConfigurationManager.getBooleanProperty("webui.suggest.enable", false))
                {
                    email.send();
                } else
                {
                    throw new MessagingException("Suggest item email not sent - webui.suggest.enable = false");
                }

                log.info(LogManager.getHeader(context, "sent_suggest",
                		                      "from=" + senderAddr));

                JSPManager.showJSP(request, response, "/suggest/suggest_ok.jsp");
            }
            catch (MessagingException me)
            {
                log.warn(LogManager.getHeader(context, "error_mailing_suggest", ""), me);
                JSPManager.showInternalError(request, response);
            }
        }
        else
        {
            // Display suggest form
            log.info(LogManager.getHeader(context, "show_suggest_form", "problem=false"));
            request.setAttribute("authenticated.email", authEmail);
            request.setAttribute("eperson.name", userName);
            JSPManager.showJSP(request, response, "/suggest/suggest.jsp"); //asd
        }
   }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
    						HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Treat as a GET
        doDSGet(context, request, response);
    }
}
