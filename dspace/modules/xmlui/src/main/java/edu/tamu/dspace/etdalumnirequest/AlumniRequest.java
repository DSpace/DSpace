package edu.tamu.dspace.etdalumnirequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import javax.mail.MessagingException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.xml.sax.SAXException;

/**
 * A form to request access to a restricted legacy thesis. Will ask the (anonymous) user for basic information and send an email off to feedback.recepient.
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 * @author James Creel (http://www.jamescreel.net)
 */

public class AlumniRequest extends AbstractDSpaceTransformer 
{
    private static Logger log = Logger.getLogger(AlumniRequest.class);
    

    public void setup(SourceResolver resolver, Map arg1, String arg2, Parameters arg3) 
    	throws ProcessingException, SAXException, IOException 
    {
	super.setup(resolver, arg1, arg2, arg3);
    }
       
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
	pageMeta.addMetadata("title").addContent("Alumni Open Access Request");
    	
        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/", message("xmlui.general.dspace_home"));
        HandleUtil.buildHandleTrail(HandleUtil.obtainHandle(objectModel), pageMeta,contextPath);
        
        pageMeta.addTrailLink(null, "Alumni Open Access Request");
    }
    
    
    public void addBody(Body body) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException
    {
	Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        MathCaptcha mc = new MathCaptcha();
        
        if (!isRequestable(context, dso))
            return;
        
        java.util.List<String> fieldsInError = new ArrayList<String>();
        
        // 0. If we just sent an email request, generate the email and set the database tables
        if (request.getParameter("submit_request") != null)
        {
            String name = request.getParameter("name");
            if (name == null || name.length() == 0)
        	fieldsInError.add("name");
            
            String phone = request.getParameter("phone");
            if (phone == null || phone.length() == 0)
        	fieldsInError.add("phone");
            
            String address = request.getParameter("address");
            if (address == null || address.length() == 0)
        	fieldsInError.add("address");
            
            String email = request.getParameter("email");
            if (email == null || email.length() == 0)
        	fieldsInError.add("email");
            
            Integer captcha = null;
            try {
        	captcha = Integer.valueOf(request.getParameter("captcha"));
            }
            catch (NumberFormatException nfe) {
		// ignore
	    }
            
            if (captcha == null || captcha != mc.rightAnswer)
        	fieldsInError.add("captcha");
        }
        
        if (request.getParameter("submit_request") != null && fieldsInError.isEmpty())
        {
            sendRequest(dso, request);
            
            Division mainDiv = body.addDivision("alum-request", "primary");
            mainDiv.setHead("Alumni Open Access Request");

            mainDiv.addPara("Request sent. Thank you, we'll be in touch.");
        }
        else 
        {
            Division mainDiv = body.addInteractiveDivision("alum-request", contextPath + "/handle/"+dso.getHandle()+"/alumni-request", Division.METHOD_POST, "primary");
            mainDiv.setHead("Alumni Open Access Request");

            mainDiv.addPara("If this is your thesis or dissertation, you can make it open-access. This will allow all visitors to view the contents of the thesis.");

            List mainList = mainDiv.addList("alum-request-list", List.TYPE_FORM);
            mainList.addLabel("Item Title");

            Text title = mainList.addItem().addText("title");
            title.setLabel("Item Title");
            title.setValue(dso.getName());
            title.setDisabled(true);

            Text name = mainList.addItem().addText("name");
            name.setLabel("Name");
            name.setValue(request.getParameter("name"));
            name.setRequired(true);
            if (fieldsInError.contains("name"))
        	name.addError("This field is required");

            Text phone = mainList.addItem().addText("phone");
            phone.setLabel("Phone Number");
            phone.setValue(request.getParameter("phone"));
            if (fieldsInError.contains("phone"))
        	phone.addError("This field is required");

            Text address = mainList.addItem().addText("address");
            address.setLabel("Postal Address");
            address.setValue(request.getParameter("address"));
            if (fieldsInError.contains("address"))
        	address.addError("This field is required");

            Text email = mainList.addItem().addText("email");
            email.setLabel("Email Address");
            email.setValue(request.getParameter("email"));
            if (fieldsInError.contains("email"))
        	email.addError("This field is required");

            TextArea comments = mainList.addItem().addTextArea("comments");
            comments.setLabel("Comments");
            comments.setValue(request.getParameter("comments"));
            comments.setHelp("Comments are optional");

            Text captcha = mainList.addItem().addText("captcha");
            captcha.setLabel("Add " + mc.a +  " + " + mc.b);
            captcha.setHelp("In order to reduce spam, please prove you are a human");
            captcha.setValue(request.getParameter("captcha"));
            if (fieldsInError.contains("captcha"))
        	captcha.addError("A correct answer is required.");

            mainList.addItem().addButton("submit_request").setValue("Request Open Access");
        }
    } 
    
    /**
     * Can an open-access alumni request be made on this object?
     * @param context
     * @param dso
     * @return
     * @throws SQLException
     */
    public static boolean isRequestable (Context context, DSpaceObject dso) throws SQLException 
    {
	// If it's not an item, ignore
        if (dso.getType() != Constants.ITEM)
        {
            return false;
        }
        
        // If the item is not in a blessed collection
        String collectionsString = ConfigurationManager.getProperty("xmlui.alumni.request.collections");
        
        if (collectionsString == null || collectionsString.length() == 0)
            return false;
        
        collectionsString = collectionsString.replaceAll(" ", "");
        java.util.List<String> collections = Arrays.asList(collectionsString.split(","));
        if (!collections.contains(((Item)dso).getOwningCollection().getHandle())) {
            return false;
        }
            
        // If the item has no restricted bitstreams
        Item item = (Item)dso;
	Bundle[] bundles = item.getBundles("ORIGINAL");
	boolean restrictions = false;
	
	for (Bundle bundle : bundles) 
	{
	    Bitstream[] bitstreams = bundle.getBitstreams();
	    for (Bitstream bitstream : bitstreams)
	    {
		if (!AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ))
		    restrictions = true;	
	    }
	}
	
	if (!restrictions)
	    return false;
	
	return true;
    }
    
    
    private void sendRequest(DSpaceObject dso, Request request) throws IOException 
    {
        Email email = Email.getEmail(I18nUtil.getEmailFilename(I18nUtil.getDefaultLocale(), "alumni_request"));
        email.addRecipient(ConfigurationManager.getProperty("feedback.recipient"));
        
        String itemURL = ConfigurationManager.getProperty("dspace.url") + "/handle/" + ((Item)dso).getHandle();
        String message = "" + request.getParameter("comments");
        
        /*
         * Parameters: 
             {0} item title
             {1} item link
             {2} requestor's name
             {3} phone number
             {4} postal address
             {5} email address
             {6} request message
         */
        email.addArgument(dso.getName());
        email.addArgument(itemURL);
        email.addArgument(request.getParameter("name"));
        email.addArgument(request.getParameter("phone"));
        email.addArgument(request.getParameter("address"));
        email.addArgument(request.getParameter("email"));
        email.addArgument(message);
        
        try {
		email.send();
	    } catch (MessagingException ex) {
		log.error("Failed sending email during Alumni Open Access Request.", ex);
		return;
	    }
    }
        
    
    public static class MathCaptcha {
	public int a;
	public int b;
	public int rightAnswer;
	
	public MathCaptcha() {
	    Date date = new Date();
	    int seed = date.getDate() * date.getMonth() + date.getYear();
	    
	    Random rand = new Random(seed);
	    a = rand.nextInt(9) + 1;
	    b = rand.nextInt(9) + 1;
	    
	    rightAnswer = a + b;
	}
    }
    

}
