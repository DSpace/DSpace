/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;

/**
 * Display to the user a simple form letting the user to request a protected item.
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class ItemRequestForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static Logger log = Logger.getLogger(ItemRequestForm.class);

    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.ItemRequestForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemRequestForm.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.ItemRequestForm.para1");

    private static final Message T_login_para =
            message("xmlui.ArtifactBrowser.ItemRequestForm.login_para");

    private static final Message T_login =
            message("xmlui.ArtifactBrowser.ItemRequestForm.login");
    
    private static final Message T_requesterEmail =
        message("xmlui.ArtifactBrowser.ItemRequestForm.requesterEmail");

    private static final Message T_requesterEmail_help =
        message("xmlui.ArtifactBrowser.ItemRequestForm.requesterEmail_help");
    
    private static final Message T_requesterEmail_error =
        message("xmlui.ArtifactBrowser.ItemRequestForm.requesterEmail.error");
    
    private static final Message T_message = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.message");
    
    private static final Message T_message_error = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.message.error");
    
    private static final Message T_requesterName = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.requesterName");
    
    private static final Message T_requesterName_error = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.requesterName.error");
    
    private static final Message T_allFiles = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.allFiles");
    
    private static final Message T_files = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.files");
    
    private static final Message T_notAllFiles = 
        message("xmlui.ArtifactBrowser.ItemRequestForm.notAllFiles");
    
    private static final Message T_submit =
        message("xmlui.ArtifactBrowser.ItemRequestForm.submit");
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
    	
        String requesterName = parameters.getParameter("requesterName","");
        String requesterEmail = parameters.getParameter("requesterEmail","");
        String allFiles = parameters.getParameter("allFiles","");
        String message = parameters.getParameter("message","");
        String bitstreamId = parameters.getParameter("bitstreamId","");
        
       return HashUtil.hash(requesterName + "-" + requesterEmail + "-" + allFiles +"-"+message+"-"+bitstreamId);
    }

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();


    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {       
        pageMeta.addMetadata("title").addContent(T_title);
 
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		
		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		if (!(dso instanceof Item)) {
			return;
		}
		Request request = ObjectModelHelper.getRequest(objectModel);
		Item item = (Item) dso;
		// Build the item viewer division.
		Division itemRequest = body.addInteractiveDivision("itemRequest-form",
				request.getRequestURI(), Division.METHOD_POST, "primary");
		itemRequest.setHead(T_head);

        // add a login link if user !loggedIn
        if (context.getCurrentUser() == null)
        {
            Para loginPara = itemRequest.addPara();
            loginPara.addContent(T_login_para);
            itemRequest.addPara().addXref(contextPath + "/login", T_login);

            // Interrupt request if the user is not authenticated, so they may come back to
            // the restricted resource afterwards.
            String header = parameters.getParameter("header", null);
            String message = parameters.getParameter("message", null);
            String characters = parameters.getParameter("characters", null);

            // Interrupt this request
            AuthenticationUtil.interruptRequest(objectModel, header, message, characters);
        } else {
            //If user has read permissions to bitstream, redirect them to bitstream, instead of restrict page
            try {
                UUID bitstreamID = UUID.fromString(parameters.getParameter("bitstreamId"));
                Bitstream bitstream = bitstreamService.find(context, bitstreamID);

                if(authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ)) {
                    String redirectURL = request.getContextPath() + "/bitstream/handle/" + item.getHandle() + "/"
                            + bitstream.getName() + "?sequence=" + bitstream.getSequenceID();

                    HttpServletResponse httpResponse = (HttpServletResponse)
                            objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                    httpResponse.sendRedirect(redirectURL);
                }
            } catch (ParameterException e) {
                log.error(e.getMessage());
            }
        }

        itemRequest.addPara(T_para1);

		String titleDC = item.getName();
		if (titleDC != null && titleDC.length() > 0)
			itemRequest.addPara(titleDC);

		List form = itemRequest.addList("form", List.TYPE_FORM);

		Text requesterName = form.addItem().addText("requesterName");
		requesterName.setLabel(T_requesterName);
		requesterName.setValue(parameters.getParameter("requesterName", ""));

		Text requesterEmail = form.addItem().addText("requesterEmail");
		requesterEmail.setLabel(T_requesterEmail);
		requesterEmail.setHelp(T_requesterEmail_help);
		requesterEmail.setValue(parameters.getParameter("requesterEmail", ""));

		Radio radio = form.addItem().addRadio("allFiles");
		String selected=!parameters.getParameter("allFiles","true").equalsIgnoreCase("false")?"true":"false";
		radio.setOptionSelected(selected);
		radio.setLabel(T_files);
		radio.addOption("true", T_allFiles);
		radio.addOption("false", T_notAllFiles);
		

		TextArea message = form.addItem().addTextArea("message");
		message.setLabel(T_message);
		message.setValue(parameters.getParameter("message", ""));
		form.addItem().addHidden("bitstreamId").setValue(parameters.getParameter("bitstreamId", ""));
		form.addItem().addButton("submit").setValue(T_submit);
		
		// if button is pressed and form is re-loaded it means some parameter is missing
		if(request.getParameter("submit")!=null){
			if(StringUtils.isEmpty(parameters.getParameter("requesterName", ""))){
				requesterName.addError(T_requesterName_error);
			}
			if(StringUtils.isEmpty(parameters.getParameter("requesterEmail", ""))){
				requesterEmail.addError(T_requesterEmail_error);
			}
			if(StringUtils.isEmpty(parameters.getParameter("message", ""))){
				message.addError(T_message_error);
			}
		}
		itemRequest.addHidden("page").setValue(parameters.getParameter("page", "unknown"));
	}
}
