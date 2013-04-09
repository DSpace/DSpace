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
import org.apache.cocoon.util.HashUtil;
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
 * Display to the user a simple form letting the user give feedback.
 * 
 * @author Scott Phillips
 * @author Dan Leehr
 */
public class FeedbackForm extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.FeedbackForm.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.FeedbackForm.trail");
    
    private static final Message T_overview_head =
        message("xmlui.ArtifactBrowser.FeedbackForm.overview.head");
    
    private static final Message T_overview_para1 =
        message("xmlui.ArtifactBrowser.FeedbackForm.overview.para1");

    private static final Message T_overview_para2 =
        message("xmlui.ArtifactBrowser.FeedbackForm.overview.para2");

    private static final Message T_overview_para3 =
        message("xmlui.ArtifactBrowser.FeedbackForm.overview.para3");

    private static final Message T_address_head =
        message("xmlui.ArtifactBrowser.FeedbackForm.address.head");

    private static final Message T_address_para1 =
        message("xmlui.ArtifactBrowser.FeedbackForm.address.para1");

    private static final Message T_ideasforum_forumlink =
        message("xmlui.ArtifactBrowser.FeedbackForm.forumLink");

    private static final Message T_contactform_head =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.head");

    private static final Message T_contactform_para1 =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.para1");
    
    private static final Message T_contactform_email =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.email");

    private static final Message T_contactform_email_help =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.email_help");
    
    private static final Message T_contactform_comments =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.comments");
    
    private static final Message T_contactform_submit =
        message("xmlui.ArtifactBrowser.FeedbackForm.contactform.submit");
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        String email = parameters.getParameter("email","");
        String comments = parameters.getParameter("comments","");
        String page = parameters.getParameter("page","unknown");
        // Changing caching key since page layout has changed in code
       return HashUtil.hash(email + "-" + comments + "-" + page + "-feedback");
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

        Division feedback = body.addInteractiveDivision("feedback-form",
                contextPath+"/feedback",Division.METHOD_POST,"primary");
        feedback.setHead(T_overview_head);
        feedback.addPara(T_overview_para1);
        feedback.addPara(T_overview_para2);
        feedback.addPara(T_overview_para3);
        feedback.addPara(T_address_head);
        
        feedback.addPara(T_address_para1);

        feedback.addPara(T_ideasforum_forumlink);

        feedback.addPara(T_contactform_head);
        feedback.addPara(T_contactform_para1);
        
        List form = feedback.addList("form",List.TYPE_FORM);
        
        Text email = form.addItem().addText("email");
        email.setLabel(T_contactform_email);
        email.setHelp(T_contactform_email_help);
        email.setValue(parameters.getParameter("email",""));
        
        TextArea comments = form.addItem().addTextArea("comments");
        comments.setLabel(T_contactform_comments);
        comments.setValue(parameters.getParameter("comments",""));
        
        form.addItem().addButton("submit").setValue(T_contactform_submit);
        
        feedback.addHidden("page").setValue(parameters.getParameter("page","unknown"));
    }
}
