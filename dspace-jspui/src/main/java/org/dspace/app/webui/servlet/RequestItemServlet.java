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
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Servlet for generate a statistisc report
 *
 * @author  Arnaldo Dantas
 * @version $Revision: 1.0 $
 */
public class RequestItemServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(RequestItemServlet.class);
    
    /** The information get by form step */
    public static final int ENTER_FORM_PAGE = 1;

    /** The link by submitter email step*/
    public static final int ENTER_TOKEN = 2;
    
    /** The link Approved generate letter step*/
    public static final int APROVE_TOKEN = 3;

    /* resume leter for request user*/
    public static final int RESUME_REQUEST = 4;

    /* resume leter for request dspace administrator*/
    public static final int RESUME_FREEACESS = 5;

    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
    
    private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
    
    private final transient RequestItemService requestItemService
             = RequestItemServiceFactory.getInstance().getRequestItemService();
    
    @Override
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

    @Override
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
        
        UUID bitstream_id= UIUtil.getUUIDParameter(request, "bitstream-id");
        
        // Title
        String title = null;
        Item item = null;
        if (StringUtils.isNotBlank(handle))
        {
            item = (Item) handleService.resolveToObject(context, handle);
            
        }
        if (item == null)
        {   
        	JSPManager.showInvalidIDError(request, response, handle, -1);
        }
        title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
        if (title == null)
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
                
                request.setAttribute("requestItem.problem", Boolean.TRUE);
                JSPManager.showJSP(request, response, "/requestItem/request-form.jsp");
                return;
            }

            try
            {
            	String token = requestItemService.createRequest(context, bitstream_id != null?
            			bitstreamService.find(context, bitstream_id):null, item, allfiles, requesterEmail, reqname, coment);
            	
                String linkedToken = getLinkTokenEmail(context, token);
                
                // All data is there, send the email
				Email email = Email.getEmail(I18nUtil.getEmailFilename(
						context.getCurrentLocale(), "request_item.author"));
				
				RequestItemAuthor author = DSpaceServicesFactory.getInstance().getServiceManager()
						.getServiceByName(
								RequestItemAuthorExtractor.class.getName(),
								RequestItemAuthorExtractor.class)
						.getRequestItemAuthor(context, item);
				
				String authorEmail = author.getEmail();
				String authorName = author.getFullName();
				
				email.addRecipient(authorEmail);

				email.addArgument(reqname);
				email.addArgument(requesterEmail);
				email.addArgument(allfiles ? I18nUtil
						.getMessage("itemRequest.all") : bitstreamService.find(context, bitstream_id).getName());
				email.addArgument(handleService.getCanonicalForm(item
                        .getHandle()));
				email.addArgument(title); // request item title
				email.addArgument(coment); // message
				email.addArgument(linkedToken);
				
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
       
        RequestItem requestItem = requestItemService.findByToken(context, token);
        // validate
        if (requestItem != null)
        {
			Item item = requestItem.getItem();
			String title = "";
			String handle = "";
			if (item != null) {
				title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
				if (title == null) {
					title = "";
				}
				handle = item.getHandle();
			}
            request.setAttribute("request-name", requestItem.getReqName());
            request.setAttribute("handle", handle);
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
		RequestItem requestItem = requestItemService.findByToken(context,
				token);

		if (requestItem != null && (yes || no)) {
			Item item = requestItem.getItem();

			String title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
			title = title != null ? title : I18nUtil
					.getMessage("jsp.general.untitled", context);
			

			EPerson submiter = item.getSubmitter();

			Object[] args = new String[]{
						requestItem.getReqName(),
						handleService.getCanonicalForm(item.getHandle()), // User
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
            RequestItem requestItem = requestItemService.findByToken( context, token);
            // validate
            if (requestItem != null)
            {
                // Token
                String subject = request.getParameter("subject");
                String message = request.getParameter("message");
                boolean accept = UIUtil.getBoolParameter(request, "accept_request");
                try
                {
                    Item item = requestItem.getItem();
                    Email email = new Email();
                    email.setSubject(subject);
                    email.setContent("{0}");
        			email.addRecipient(requestItem.getReqEmail());
                    email.addArgument(message);
                    
					// add attach
					if (accept) {
						if (requestItem.getBitstream() == null) {
							List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
							for (Bundle b : bundles) {
								List<Bitstream> bbitstreams = b.getBitstreams();
								for (Bitstream bitstream : bbitstreams) {
									if (!bitstream.getFormat(context).isInternal()
											&& authorizeService.authorizeActionBoolean(context, null, bitstream, Constants.READ, false)) {
										email.addAttachment(
												bitstreamService
														.retrieve(
                                                                context,
                                                                bitstream),
												bitstream.getName(),
												bitstream.getFormat(context)
														.getMIMEType());
									}
								}
							}
						} else {
							Bitstream bit = requestItem.getBitstream();
							email.addAttachment(bitstreamService.retrieve(context, bit), bit.getName(),
									bit.getFormat(context).getMIMEType());
						}
					}
                    email.send();

                    requestItem.setAccept_request(accept);
                    requestItem.setDecision_date(new Date());
                    requestItemService.update(context, requestItem);

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
		RequestItem requestItem = requestItemService.findByToken(context,
				token);

		if (requestItem != null && free) {
			try {
				Item item = requestItem.getItem();

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

				email.addArgument(requestItem.getBitstream().getName());
				email.addArgument(handleService.getCanonicalForm(item
                        .getHandle()));
				email.addArgument(requestItem.getToken());
				email.addArgument(name);
				email.addArgument(mail);

				email.send();

				log.info(LogManager.getHeader(context, "sent_adm_requestItem",
						"token=" + requestItem.getToken()
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
	
    /**
     * Get the link to the author in RequestLink email.
     * 
     * @param context
     *            the context object
     * @param token
     *            the token
     * 
     * @return link based on the token
     * 
     * @throws java.sql.SQLException 
     */
    public static String getLinkTokenEmail(Context context, String token)
            throws SQLException
    {
        String base = ConfigurationManager.getProperty("dspace.url");

        String specialLink = (new StringBuffer()).append(base)
                .append(base.endsWith("/") ? "" : "/")
                .append("request-item")
                .append("?step=")
                .append(RequestItemServlet.ENTER_TOKEN)
                .append("&token=")
                .append(token)
                .toString();
        
        return specialLink;
    }
}
