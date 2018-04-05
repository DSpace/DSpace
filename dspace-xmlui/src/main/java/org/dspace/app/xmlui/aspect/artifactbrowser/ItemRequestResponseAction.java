/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class ItemRequestResponseAction extends AbstractAction
{
    protected RequestItemService requestItemService = RequestItemServiceFactory.getInstance().getRequestItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();

	/** log4j log */
    private static Logger log = Logger.getLogger(ItemRequestResponseAction.class);

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
       
        String token = parameters.getParameter("token","");
        String decision = request.getParameter("decision");
        String isSent = request.getParameter("isSent");
        //contactPerson:requester or contactPerson:author
        String contactPerson = request.getParameter("contactPerson");

        Context context = ContextUtil.obtainContext(objectModel);
        request.setAttribute("token", token);

        RequestItem requestItem = requestItemService.findByToken(context, token);
        String title;
        Item item = requestItem.getItem();
        String titleDC = item.getName();
        if (titleDC != null && titleDC.length() > 0)
        	title=titleDC;
        else
        	title="untitled";
        
        String button="";
        // Buttons on the page
        if(request.getParameter("send")!=null){
        	decision="true";
        	button="send";
        } else if(request.getParameter("dontSend")!=null){
        	decision="false";
        	button="dontSend";
        } else if(request.getParameter("contactRequester") !=null) {
            button = "contactRequester";
        } else if (request.getParameter("contactAuthor") != null) {
            button = "contactAuthor";
        }

        if(request.getParameter("mail")!=null){
        	button="mail";
        }else if(request.getParameter("back")!=null){
        	button="back";
        }
        if(request.getParameter("openAccess")!=null){
        	button="openAccess";
        }
         
        if(button.equals("mail")) {
            //Send mail
            if(StringUtils.isNotEmpty(decision) && decision.equals("true")){
        	    processSendDocuments(context,request,requestItem,item,title);
        	    isSent="true";
            } else if(StringUtils.isNotEmpty(decision) && decision.equals("false")) {
                processDeny(context, request, requestItem, item, title);
                isSent = "true";
            } else if(StringUtils.isNotEmpty(contactPerson) && contactPerson.equals("requester")) {
                log.info("ContactRequester");
                processContactRequester(request, requestItem);
                decision = null;
                isSent = "notify";
            } else if(StringUtils.isNotEmpty(contactPerson) && contactPerson.equals("author")) {
                processContactAuthor(request);
                decision = null;
                isSent = "notify";
            }
        } else if(button.equals("openAccess")){
        	if(processOpenAccessRequest(context,request,requestItem,item,title)){
            	// se acabo el flujo
            	return null;
        	}
        }else if(button.equals("back")){
        	decision=null;
            contactPerson=null;
        } else if(button.equals("contactRequester")) {
            decision=null;
            isSent=null;
            contactPerson = "requester";
        } else if(button.equals("contactAuthor")) {
            decision=null;
            isSent=null;
            contactPerson = "author";
        }

        
		Map<String, String> map = new HashMap<String, String>();
		map.put("decision", decision);
		map.put("token", token);
		map.put("isSent", isSent);
        map.put("contactPerson", contactPerson);
		map.put("title", title);
		map.put("name", request.getParameter("name"));
		map.put("email",request.getParameter("email"));
		return map;
    }



    private boolean processOpenAccessRequest(Context context,Request request, RequestItem requestItem,Item item,String title) throws SQLException, IOException, MessagingException {
    	String name = request.getParameter("name");
    	String mail = request.getParameter("email");

    	if(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(mail)){
            RequestItemAuthor requestItemAuthor = DSpaceServicesFactory.getInstance().getServiceManager()
                    .getServiceByName(RequestItemAuthorExtractor.class.getName(),
                            RequestItemAuthorExtractor.class)
                    .getRequestItemAuthor(context, item);

	    	Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "request_item.admin"));
	        email.addRecipient(requestItemAuthor.getEmail());
	        
	        email.addArgument(requestItem.getBitstream().getName());
	        email.addArgument(handleService.getCanonicalForm(item.getHandle()));
	        email.addArgument(requestItem.getToken());
	        email.addArgument(name);    
	        email.addArgument(mail);   
	        
	        email.send();
	        return true;
    	}
    	return false;
	}

	private void processSendDocuments(Context context,Request request, RequestItem requestItem,Item item,String title) throws SQLException, MessagingException, IOException {
    	String message = request.getParameter("message");
    	String subject = request.getParameter("subject");
    	
    	Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
		email.addRecipient(requestItem.getReqEmail());
        email.addArgument(message);
       
        if (requestItem.isAllfiles()){
            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");
            for (Bundle bundle : bundles) {
                List<Bitstream> bitstreams = bundle.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    if (!bitstream.getFormat(context).isInternal() /*&& RequestItemManager.isRestricted(context, bitstreams[k])*/) {
                        email.addAttachment(bitstreamStorageService.retrieve(context, bitstream), bitstream.getName(), bitstream.getFormat(context).getMIMEType());
                    }
                }
            }
        } else {
            Bitstream bit = requestItem.getBitstream();
            email.addAttachment(bitstreamStorageService.retrieve(context, requestItem.getBitstream()), bit.getName(), bit.getFormat(context).getMIMEType());
        }     
        
        email.send();

        requestItem.setDecision_date(new Date());
        requestItem.setAccept_request(true);
        requestItemService.update(context, requestItem);
	}

	private void processDeny(Context context,Request request, RequestItem requestItem,Item item,String title) throws SQLException, IOException, MessagingException {
		String message = request.getParameter("message");
    	String subject = request.getParameter("subject");
    	        
    	Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
		email.addRecipient(requestItem.getReqEmail());
        email.addArgument(message);
        email.send();
        
        requestItem.setDecision_date(new Date());
        requestItem.setAccept_request(false);
        requestItemService.update(context, requestItem);
	}

    /**
     * Send an email back to the requester, letting them know request is being processed.
     * Use case is for helpdesk user to contact requester
     * @param request
     * @param requestItem
     * @throws IOException
     * @throws MessagingException
     */
    private void processContactRequester(Request request, RequestItem requestItem) throws IOException, MessagingException {
        String message = request.getParameter("message");
        String subject = request.getParameter("subject");

        Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
        email.addRecipient(requestItem.getReqEmail());
        email.addArgument(message);

        email.send();
    }

    /**
     * Send an email to the author, asking them to consider allowing access.
     * Use case is for helpdesk user to contact author
     * @param request
     * @throws IOException
     * @throws MessagingException
     */
    private void processContactAuthor(Request request) throws IOException, MessagingException {
        String message = request.getParameter("message");
        String subject = request.getParameter("subject");

        Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
        email.addRecipient(request.getParameter("toEmail"));
        email.addArgument(message);

        email.send();
    }
}
