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
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * @author Adán Roman Ruiz at arvo.es
 */
public class JuzgarForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.JuzgarForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.JuzgarForm.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.JuzgarForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.JuzgarForm.para1");
    
    private static final Message T_email =
        message("xmlui.ArtifactBrowser.JuzgarForm.email");

    private static final Message T_email_help =
        message("xmlui.ArtifactBrowser.JuzgarForm.email_help");
    
    private static final Message T_submit =
        message("xmlui.ArtifactBrowser.JuzgarForm.submit");
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        String email = parameters.getParameter("email","");
        String handle = parameters.getParameter("handle","");
        String mensaje = parameters.getParameter("mensaje","");
        
       return HashUtil.hash(email + "-" + handle + "-" + mensaje);
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

	String mensaje = parameters.getParameter("mensaje","");
	String handle = parameters.getParameter("handle","");
	  
        // Build the item viewer division.
        Division feedback = body.addInteractiveDivision("juzgar-form",
                contextPath+"/juzgarRequest/"+handle,Division.METHOD_POST,"primary");
        
        feedback.setHead(T_head);
        
        feedback.addPara(T_para1);
        
        if (!StringUtils.isBlank(mensaje)){
            feedback.addPara( message(mensaje));
        }
        
        List form = feedback.addList("form",List.TYPE_FORM);
        
        Text email = form.addItem().addText("email");
        email.setAutofocus("autofocus");
        email.setLabel(T_email);
        email.setHelp(T_email_help);
        email.setValue(parameters.getParameter("email",""));
               
        form.addItem().addButton("submit").setValue(T_submit);
       
        
        feedback.addHidden("page").setValue(parameters.getParameter("page","unknown"));
    }
}
