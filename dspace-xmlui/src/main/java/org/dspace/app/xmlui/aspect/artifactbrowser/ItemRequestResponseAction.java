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
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.HandleManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.utils.DSpace;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
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
	/** log4j log */
    private static Logger log = Logger.getLogger(ItemRequestResponseAction.class);

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
       
        String token = parameters.getParameter("token","");
        String decision = request.getParameter("decision");
        String isSent = request.getParameter("isSent");
        String message = request.getParameter("message");

        //contactPerson:requester or contactPerson:author
        String contactPerson = request.getParameter("contactPerson");

        Context context = ContextUtil.obtainContext(objectModel);
        request.setAttribute("token", token);

        RequestItem requestItem = RequestItem.findByToken(context, token);
        String title;
        Item item = Item.find(context, requestItem.getItemID());
        Metadatum[] titleDC = item.getDC("title", null, Item.ANY);
        if (titleDC != null || titleDC.length > 0) 
        	title=titleDC[0].value; 
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
            RequestItemAuthor requestItemAuthor = new DSpace()
                    .getServiceManager()
                    .getServiceByName(RequestItemAuthorExtractor.class.getName(),
                            RequestItemAuthorExtractor.class)
                    .getRequestItemAuthor(context, item);

	    	Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "request_item.admin"));
	        email.addRecipient(requestItemAuthor.getEmail());
	        
	        email.addArgument(Bitstream.find(context,requestItem.getBitstreamId()).getName());
	        email.addArgument(HandleManager.getCanonicalForm(item.getHandle()));
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
            Bundle[] bundles = item.getBundles("ORIGINAL");
            for (int i = 0; i < bundles.length; i++){
                Bitstream[] bitstreams = bundles[i].getBitstreams();
                for (int k = 0; k < bitstreams.length; k++){
                    if (!bitstreams[k].getFormat().isInternal() /*&& RequestItemManager.isRestricted(context, bitstreams[k])*/){
                        email.addAttachment(BitstreamStorageManager.retrieve(context, bitstreams[k].getID()), bitstreams[k].getName(), bitstreams[k].getFormat().getMIMEType());
                    }
                }
            }
        } else {
            Bitstream bit = Bitstream.find(context,requestItem.getBitstreamId());
            email.addAttachment(BitstreamStorageManager.retrieve(context, requestItem.getBitstreamId()), bit.getName(), bit.getFormat().getMIMEType());
        }     
        
        email.send();

        requestItem.setDecision_date(new Date());
        requestItem.setAccept_request(true);
        requestItem.update(context);
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
        requestItem.update(context);
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
