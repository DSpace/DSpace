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

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Display to the user a form to request change of permissions of a item.
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class ItemRequestChangeStatusForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.para1");
    
    private static final Message T_name = 
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.name");
    
    private static final Message T_email = 
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.email");
    
    private static final Message T_name_error = 
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.name.error");
    
    private static final Message T_email_error = 
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.email.error");
    
    private static final Message T_changeToOpen =
        message("xmlui.ArtifactBrowser.ItemRequestChangeStatusForm.changeToOpen");
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        String token = parameters.getParameter("token","");
        String name = parameters.getParameter("name","");
        String email = parameters.getParameter("email","");
        Request request = ObjectModelHelper.getRequest(objectModel);
        String openAccess = request.getParameter("openAccess");
        return HashUtil.hash(token+name+email+openAccess);
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

        // Build the item viewer division.
        Division itemRequest = body.addInteractiveDivision("itemRequest-form",
                request.getRequestURI(),Division.METHOD_POST,"primary");
        itemRequest.setHead(T_head);
        
        itemRequest.addPara(T_para1);
                
        List form = itemRequest.addList("form",List.TYPE_FORM);
        
        Text name = form.addItem().addText("name");
        name.setLabel(T_name);
        name.setValue(parameters.getParameter("name",""));
        
        Text mail = form.addItem().addText("email");
        mail.setLabel(T_email);
        mail.setValue(parameters.getParameter("email",""));
        
        if(request.getParameter("openAccess")!=null){
			if(StringUtils.isEmpty(parameters.getParameter("name", ""))){
				name.addError(T_name_error);
			}
			if(StringUtils.isEmpty(parameters.getParameter("email", ""))){
				mail.addError(T_email_error);
			}
		}
       // mail.setValue(parameters.getParameter("mail",""));
        form.addItem().addHidden("isSent").setValue("true");
        form.addItem().addButton("openAccess").setValue(T_changeToOpen);
    }
}
