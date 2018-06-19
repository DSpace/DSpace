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
import java.text.MessageFormat;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItemAuthor;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form letting the user send a document with a message.
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class ItemRequestResponseTrueForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.para1");
    
    private static final Message T_mail =
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.mail");

    private static final Message T_back =
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.back");
    
    private static final Message T_message = 
        message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.message");

    private static final Message T_subject = 
            message("xmlui.ArtifactBrowser.ItemRequestResponseTrueForm.subject");

    protected RequestItemService requestItemService = RequestItemServiceFactory.getInstance().getRequestItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {      
		String token = parameters.getParameter("token", "");
		String decision = parameters.getParameter("decision", "");

		return HashUtil.hash(token+"-"+decision);
    }

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
            UIException, SQLException, IOException, AuthorizeException
    {
    	Request request = ObjectModelHelper.getRequest(objectModel);
		Context context = ContextUtil.obtainContext(objectModel);

        String token = (String) request.getAttribute("token");
        RequestItem requestItem = requestItemService.findByToken(context, token);

		String title;
		Item item = requestItem.getItem();
		String titleDC = item.getName();
		if (titleDC != null && titleDC.length() > 0)
			title = titleDC;
		else
			title = "untitled";
		
		RequestItemAuthor author = DSpaceServicesFactory.getInstance().getServiceManager()
				.getServiceByName(RequestItemAuthorExtractor.class.getName(),
						RequestItemAuthorExtractor.class)
				.getRequestItemAuthor(context, item);

		Object[] args = new String[]{
					requestItem.getReqName(), // User
                    handleService.getCanonicalForm(item.getHandle()), // URL
					title, // request item title
					author.getFullName(),
					author.getEmail()
				};
		
		String subject = I18nUtil.getMessage("itemRequest.response.subject.approve", context);
		String messageTemplate = MessageFormat.format(I18nUtil.getMessage("itemRequest.response.body.approve", context), args);
		
        Division itemRequest = body.addInteractiveDivision("itemRequest-form", request.getRequestURI(),Division.METHOD_POST,"primary");
        itemRequest.setHead(T_head);
        
        itemRequest.addPara(T_para1);
                
        List form = itemRequest.addList("form",List.TYPE_FORM);
        Text subj = form.addItem().addText("subject");
        subj.setLabel(T_subject);
        subj.setValue(subject);
        subj.setSize(60);
        TextArea message = form.addItem().addTextArea("message");
        message.setSize(20, 0);
        message.setLabel(T_message);
        message.setValue(parameters.getParameter("message",messageTemplate));
        form.addItem().addHidden("decision").setValue(parameters.getParameter("decision",""));
        form.addItem().addButton("back").setValue(T_back);
        form.addItem().addButton("mail").setValue(T_mail);
    }
}
