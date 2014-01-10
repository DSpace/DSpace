/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;


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
        Context context = ContextUtil.obtainContext(objectModel);
        request.setAttribute("token", token);
        
        TableRow requestItem = DatabaseManager.findByUnique(context, "requestitem", "token", token);
        String title;
        Item item = Item.find(context, requestItem.getIntColumn("item_id"));
        DCValue[] titleDC = item.getDC("title", null, Item.ANY);
        if (titleDC != null || titleDC.length > 0) 
        	title=titleDC[0].value; 
        else
        	title="untitled";
        
        String button="";
        // Botones de las paginas:
        if(request.getParameter("send")!=null){
        	decision="true";
        	button="send";
        }else if(request.getParameter("dontSend")!=null){
        	decision="false";
        	button="dontSend";
        }
        if(request.getParameter("mail")!=null){
        	button="mail";
        }else if(request.getParameter("back")!=null){
        	button="back";
        }
        if(request.getParameter("openAccess")!=null){
        	button="openAccess";
        }
         
        if(button.equals("mail")&& StringUtils.isNotEmpty(decision) && decision.equals("true")){
        	processSendDocuments(context,request,requestItem,item,title);
        	isSent="true";
        }else if(button.equals("mail")&& StringUtils.isNotEmpty(decision) && decision.equals("false")){
        	processDeny(context,request,requestItem,item,title);
        	isSent="true";
        }else if(button.equals("openAccess")){
        	if(processOpenAccessRequest(context,request,requestItem,item,title)){
            	// se acabo el flujo
            	return null;
        	}
        }else if(button.equals("back")){
        	decision=null;
        }

        
		Map<String, String> map = new HashMap<String, String>();
		map.put("decision", decision);
		map.put("token", token);
		map.put("isSent", isSent);
		map.put("title", title);
		map.put("name", request.getParameter("name"));
		map.put("email",request.getParameter("email"));
		return map;
    }



    private boolean processOpenAccessRequest(Context context,Request request, TableRow requestItem,Item item,String title) throws SQLException, IOException, MessagingException {
    	String name = request.getParameter("name");
    	String mail = request.getParameter("email");
    	if(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(mail)){
    	    String emailRequest;
            EPerson submiter = item.getSubmitter();
            if(submiter!=null){
     	    	emailRequest=submiter.getEmail();
            }else{
                emailRequest=ConfigurationManager.getProperty("mail.helpdesk");
            }
            if(emailRequest==null){
                emailRequest=ConfigurationManager.getProperty("mail.admin");
            }
	    	Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "request_item.admin"));
	        email.addRecipient(emailRequest);
	        
	        email.addArgument(Bitstream.find(context,requestItem.getIntColumn("bitstream_id")).getName());
	        email.addArgument(HandleManager.getCanonicalForm(item.getHandle()));
	        email.addArgument(requestItem.getStringColumn("token"));
	        email.addArgument(name);    
	        email.addArgument(mail);   
	        
	        email.send();
	        return true;
    	}
    	return false;
	}

	private void processSendDocuments(Context context,Request request, TableRow requestItem,Item item,String title) throws SQLException, MessagingException, IOException {
    	String message = request.getParameter("message");
    	String subject = request.getParameter("subject");
    	
    	Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
		email.addRecipient(requestItem.getStringColumn("request_email"));
        email.addArgument(message);
       
        if (requestItem.getBooleanColumn("allfiles")){
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
            Bitstream bit = Bitstream.find(context,requestItem.getIntColumn("bitstream_id"));
            email.addAttachment(BitstreamStorageManager.retrieve(context, requestItem.getIntColumn("bitstream_id")), bit.getName(), bit.getFormat().getMIMEType());
        }     
        
        email.send();
        requestItem.setColumn("decision_date",new Date());
        requestItem.setColumn("accept_request",true);
        DatabaseManager.update(context, requestItem);
	}

	private void processDeny(Context context,Request request, TableRow requestItem,Item item,String title) throws SQLException, IOException, MessagingException {
		String message = request.getParameter("message");
    	String subject = request.getParameter("subject");
    	        
    	Email email = new Email();
        email.setSubject(subject);
        email.setContent("{0}");
		email.addRecipient(requestItem.getStringColumn("request_email"));
        email.addArgument(message);
        email.send();
        
        requestItem.setColumn("decision_date",new Date());
        requestItem.setColumn("accept_request",false);
        DatabaseManager.update(context, requestItem);
	}
}
