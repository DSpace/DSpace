/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.RequestItemManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;

/**
 * Servlet for generate a statistisc report
 *
 * @author  Arnaldo Dantas
 * @version $Revision: 1.0 $
 */
public class RequestItemServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(RequestItemServlet.class);
    
    /** The information get by form step */
    public static final int ENTER_FORM_PAGE = 1;

    /** The link by submmiter email step*/
    public static final int ENTER_TOKEN = 2;
    
    /** The link Aproved genarate letter step*/
    public static final int APROVE_TOKEN = 3;

    /* resume leter for request user*/
    public static final int RESUME_REQUEST = 4;

    /* resume leter for request dspace administrator*/
    public static final int RESUME_FREEACESS = 5;

    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // First get the step
        int step = UIUtil.getIntParameter(request, "step");

        try
        {
	        switch (step)
	        {
	        case ENTER_FORM_PAGE:
	            processForm(context, request, response);
	            break;
	
	        case ENTER_TOKEN:
	            processToken(context, request, response);
	            break;
	
	        case APROVE_TOKEN:
	            processLetter(context, request, response);
	            break;
	
	        case RESUME_REQUEST:
	            processAttach(context, request, response);
	            break;
	
	        case RESUME_FREEACESS:
	            processAdmin(context, request, response);
	            break;
	            
	        default:
	            processForm(context, request, response);
	        }
	        context.complete();
        }
        catch (MessagingException e)
        {
        	throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Treat as a GET
        doDSGet(context, request, response);
    }
 
    private void processForm (Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
    	boolean showRequestCopy = false;
		if ("all".equalsIgnoreCase(ConfigurationManager.getProperty("request.item.type")) || 
				("logged".equalsIgnoreCase(ConfigurationManager.getProperty("request.item.type")) &&
						context.getCurrentUser() != null))
		{
			showRequestCopy = true;
		}
		
		if (!showRequestCopy)
		{
			throw new AuthorizeException("The request copy feature is disabled");
		}
		
        // handle
        String handle = request.getParameter("handle");
        
        String bitstream_id=request.getParameter("bitstream-id");
        
        // Title
        String title = null;
        Item item = null;
        if (StringUtils.isNotBlank(handle))
        {
            item = (Item) HandleManager.resolveToObject(context, handle);
            
        }
        if (item == null)
        {   
        	JSPManager.showInvalidIDError(request, response, handle, -1);
        }
        DCValue[] titleDC = item.getDC("title", null, Item.ANY);
        if (titleDC != null || titleDC.length > 0)
        {
            title = titleDC[0].value;
        }
        else
		{
			title = I18nUtil.getMessage("jsp.general.untitled", context);
		}
          
        // User email from context
        String requesterEmail = request.getParameter("email");
        EPerson currentUser = context.getCurrentUser();
        String userName = null;
        
        if (currentUser != null)
        {
            requesterEmail = currentUser.getEmail();
            userName = currentUser.getFullName();
        }
        
        if (request.getParameter("submit") != null)
        {
            String reqname = request.getParameter("reqname");
            String coment = request.getParameter("coment");
            if (coment == null || coment.equals(""))
                coment = "";
            boolean allfiles = "true".equals(request.getParameter("allfiles"));
            
            // Check all data is there
            if (requesterEmail == null || requesterEmail.equals("") ||
                reqname == null || reqname.equals("")) 
            {
                request.setAttribute("handle",handle);
                request.setAttribute("bitstream-id", bitstream_id);
                request.setAttribute("reqname", reqname);
                request.setAttribute("email", requesterEmail);
                request.setAttribute("coment", coment);
                request.setAttribute("title", title); 
                request.setAttribute("allfiles", allfiles?"true":null); 
                
                request.setAttribute("requestItem.problem", new Boolean(true));
                JSPManager.showJSP(request, response, "/requestItem/request-form.jsp");
                return;
            }

            try
            {
                // All data is there, send the email
				Email email = Email.getEmail(I18nUtil.getEmailFilename(
						context.getCurrentLocale(), "request_item.author"));
				
				RequestItemAuthor author = new DSpace()
						.getServiceManager()
						.getServiceByName(
								RequestItemAuthorExtractor.class.getName(),
								RequestItemAuthorExtractor.class)
						.getRequestItemAuthor(context, item);
				
				String authorEmail = author.getEmail();
				String authorName = author.getFullName();
				String emailRequest;
				
				if (authorEmail != null) {
					emailRequest = authorEmail;
				} else {
					emailRequest = ConfigurationManager
							.getProperty("mail.helpdesk");
				}
				
				if (emailRequest == null) {
					emailRequest = ConfigurationManager
							.getProperty("mail.admin");
				}
				email.addRecipient(emailRequest);

				email.addArgument(reqname);
				email.addArgument(requesterEmail);
				email.addArgument(allfiles ? I18nUtil
						.getMessage("itemRequest.all") : Bitstream.find(
						context, Integer.parseInt(bitstream_id)).getName());
				email.addArgument(HandleManager.getCanonicalForm(item
						.getHandle()));
				email.addArgument(title); // request item title
				email.addArgument(coment); // message
				email.addArgument(RequestItemManager.getLinkTokenEmail(context,
						bitstream_id, item.getID(), requesterEmail, reqname,
						allfiles));
				
				email.addArgument(authorName); // corresponding author name
				email.addArgument(authorEmail); // corresponding author email
				email.addArgument(ConfigurationManager
						.getProperty("dspace.name"));
				email.addArgument(ConfigurationManager
						.getProperty("mail.helpdesk"));
				email.setReplyTo(requesterEmail);
				email.send();

                log.info(LogManager.getHeader(context,
                    "sent_email_requestItem",
                    "submitter_id=" + requesterEmail
                        + ",bitstream_id="+bitstream_id
                        + ",requestEmail="+requesterEmail));

                request.setAttribute("handle", handle);
                JSPManager.showJSP(request, response,
                    "/requestItem/request-send.jsp");
            }
            catch (MessagingException me)
            {
                log.warn(LogManager.getHeader(context,
                    "error_mailing_requestItem",
                    ""), me);
               JSPManager.showInternalError(request, response);
            }
        }
        else
        {
            // Display request copy form
            log.info(LogManager.getHeader(context,
                "show_requestItem_form",
                "problem=false"));
            request.setAttribute("handle", handle);
            request.setAttribute("bitstream-id", bitstream_id);
            request.setAttribute("email", requesterEmail);
            request.setAttribute("reqname", userName);
            request.setAttribute("title", title);
            request.setAttribute("allfiles", "true");
            JSPManager.showJSP(request, response, "/requestItem/request-form.jsp"); 
        }
   }


    /* receive token
     * get all request data by token
     * send email to request user
     */
   private void processToken (Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
       // Token
        String token = request.getParameter("token");
       
        TableRow requestItem = RequestItemManager.getRequestbyToken( context, token);
        // validate
        if (requestItem != null)
        {
            Item item = Item.find(context, requestItem.getIntColumn("item_id"));
            String title = "";
             if (item != null)
            {   
                DCValue[] titleDC = item.getDC("title", null, Item.ANY);
                if (titleDC != null || titleDC.length > 0) 
                    title = titleDC[0].value; 
            }
            request.setAttribute("request-name", requestItem.getStringColumn("request_name"));
            request.setAttribute("handle", item.getHandle());
            request.setAttribute("title", title);
            
            JSPManager.showJSP(request, response,
                    "/requestItem/request-information.jsp");
        }else{
            JSPManager.showInvalidIDError(request, response, token, -1);
        }
        
   }
   
	/*
	 * receive approvation and generate a letter
	 * get all request data by token
	 * send email to request user
	 */
	private void processLetter(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException, MessagingException {
		// Token
		String token = request.getParameter("token");
		boolean yes = request.getParameter("submit_yes") != null;
		boolean no = request.getParameter("submit_no") != null;

		// get token, get register, get email template, format email, get
		// message to jsp
		TableRow requestItem = RequestItemManager.getRequestbyToken(context,
				token);

		if (requestItem != null && (yes || no)) {
			Item item = Item.find(context, requestItem.getIntColumn("item_id"));

			DCValue[] titleDC = item.getDC("title", null, Item.ANY);
			String title = titleDC.length > 0 ? titleDC[0].value : I18nUtil
					.getMessage("jsp.general.untitled", context);
			

			EPerson submiter = item.getSubmitter();

			Object[] args = new String[]{
						requestItem.getStringColumn("request_name"),
						HandleManager.getCanonicalForm(item.getHandle()), // User
						title, // request item title
						submiter.getFullName(), // # submmiter name
						submiter.getEmail() // # submmiter email
					};
			
			String subject = I18nUtil.getMessage("itemRequest.response.subject."
					+ (yes ? "approve" : "reject"), context);
			String message = MessageFormat.format(I18nUtil.getMessage("itemRequest.response.body."
					+ (yes ? "approve" : "reject"), context), args);
			
			// page
			request.setAttribute("response", yes);
			request.setAttribute("subject", subject);
			request.setAttribute("message", message);
			JSPManager.showJSP(request, response,
					"/requestItem/request-letter.jsp");
		} else {
			JSPManager.showInvalidIDError(request, response, token, -1);
		}
	}

	/*
	 * receive token 
	 * get all request data by token 
	 * send email to request user
	 */
   private void processAttach (Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
       // Token
        String token = request.getParameter("token");
        
        //buttom
        boolean submit_next = (request.getParameter("submit_next") != null);
       
        if (submit_next)
        {
            TableRow requestItem = RequestItemManager.getRequestbyToken( context, token);
            // validate
            if (requestItem != null)
            {
                // Token
                String subject = request.getParameter("subject");
                String message = request.getParameter("message");
                boolean accept = UIUtil.getBoolParameter(request, "accept_request");
                try
                {
                    Item item = Item.find(context, requestItem.getIntColumn("item_id"));
                    Email email = new Email();
                    email.setSubject(subject);
                    email.setContent("{0}");
        			email.addRecipient(requestItem.getStringColumn("request_email"));
                    email.addArgument(message);
                    
					// add attach
					if (accept) {
						if (requestItem.getBooleanColumn("allfiles")) {
							Bundle[] bundles = item.getBundles("ORIGINAL");
							for (int i = 0; i < bundles.length; i++) {
								Bitstream[] bitstreams = bundles[i]
										.getBitstreams();
								for (int k = 0; k < bitstreams.length; k++) {
									if (!bitstreams[k].getFormat().isInternal()
											&& RequestItemManager.isRestricted(
													context, bitstreams[k])) {
										email.addAttachment(
												BitstreamStorageManager
														.retrieve(
																context,
																bitstreams[k]
																		.getID()),
												bitstreams[k].getName(),
												bitstreams[k].getFormat()
														.getMIMEType());
									}
								}
							}
						} else {
							Bitstream bit = Bitstream.find(context,
									requestItem.getIntColumn("bitstream_id"));
							email.addAttachment(BitstreamStorageManager
									.retrieve(context, requestItem
											.getIntColumn("bitstream_id")), bit
									.getName(), bit.getFormat().getMIMEType());
						}
					}
                    email.send();

                    requestItem.setColumn("accept_request",accept);
                    requestItem.setColumn("decision_date",new Date());
                    DatabaseManager.update(context, requestItem);

                    log.info(LogManager.getHeader(context,
                        "sent_attach_requestItem",
                        "token=" + token));

                    JSPManager.showJSP(request, response,
                        "/requestItem/request-free-access.jsp");
                }
                catch (MessagingException me)
                {
                    log.warn(LogManager.getHeader(context,
                        "error_mailing_requestItem",
                        ""), me);
                   JSPManager.showInternalError(request, response);
                }            
			} else
				JSPManager.showInvalidIDError(request, response, null, -1);
		} else {
			processToken(context, request, response);
		}
   }

	/*
	 * receive approvation and generate a letter 
	 * get all request data by token
	 * send email to request user
	 */
	private void processAdmin(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
		// Token
		String token = request.getParameter("token");
		boolean free = request.getParameter("submit_free") != null;
		String name = request.getParameter("name");
		String mail = request.getParameter("email");
		// get token, get register, get email template, format email, get
		// message to jsp
		TableRow requestItem = RequestItemManager.getRequestbyToken(context,
				token);

		if (requestItem != null && free) {
			try {
				Item item = Item.find(context,
						requestItem.getIntColumn("item_id"));

				String emailRequest;
				EPerson submiter = item.getSubmitter();
				if (submiter != null) {
					emailRequest = submiter.getEmail();
				} else {
					emailRequest = ConfigurationManager
							.getProperty("mail.helpdesk");
				}
				if (emailRequest == null) {
					emailRequest = ConfigurationManager
							.getProperty("mail.admin");
				}
				Email email = Email.getEmail(I18nUtil.getEmailFilename(
						context.getCurrentLocale(), "request_item.admin"));
				email.addRecipient(emailRequest);

				email.addArgument(Bitstream.find(context,
						requestItem.getIntColumn("bitstream_id")).getName());
				email.addArgument(HandleManager.getCanonicalForm(item
						.getHandle()));
				email.addArgument(requestItem.getStringColumn("token"));
				email.addArgument(name);
				email.addArgument(mail);

				email.send();

				log.info(LogManager.getHeader(context, "sent_adm_requestItem",
						"token=" + requestItem.getStringColumn("token")
								+ "item_id=" + item.getID()));

				JSPManager.showJSP(request, response,
						"/requestItem/response-send.jsp");
			} catch (MessagingException me) {
				log.warn(LogManager.getHeader(context,
						"error_mailing_requestItem", ""), me);
				JSPManager.showInternalError(request, response);
			}
		} else {
			JSPManager.showInvalidIDError(request, response, token, -1);
		}
   }
}