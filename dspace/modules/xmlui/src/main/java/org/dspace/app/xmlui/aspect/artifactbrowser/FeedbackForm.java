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
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Date;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form letting the user give feedback.
 * 
 * @author Scott Phillips
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
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.FeedbackForm.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.FeedbackForm.para1");
    
    private static final Message T_email =
        message("xmlui.ArtifactBrowser.FeedbackForm.email");

    private static final Message T_email_help =
        message("xmlui.ArtifactBrowser.FeedbackForm.email_help");
    
    private static final Message T_comments = 
        message("xmlui.ArtifactBrowser.FeedbackForm.comments");
    
    private static final Message T_submit =
        message("xmlui.ArtifactBrowser.FeedbackForm.submit");
    
    // Begin UMD Customization
    // Customization for LIBDRUM-563
    private static final String FORM_HASH = "wufoo.feedback.formHash";

    private static final String EMAIL_FIELD = "wufoo.feedback.field.email";

    private static final String PAGE_FIELD = "wufoo.feedback.field.page";

    private static final String AGENT_FIELD = "wufoo.feedback.field.agent";

    private static final String EPERSON_FIELD = "wufoo.feedback.field.eperson";

    private static final String DATE_FIELD = "wufoo.feedback.field.date";

    private static final String SESSION_FIELD = "wufoo.feedback.field.session";

    private static final String HOST_FIELD = "wufoo.feedback.field.host";

    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    // End Customization for LIBDRUM-563

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        
        String email = parameters.getParameter("email","");
        String comments = parameters.getParameter("comments","");
        String page = parameters.getParameter("page","unknown");
        
       return HashUtil.hash(email + "-" + comments + "-" + page);
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

        // Begin UMD Customization
        // Customization for LIBDRUM-563
        // Add metadata needed for Wufoo feedback form
        String wufooFormHash = configurationService.getProperty(FORM_HASH);
        if (StringUtils.isNotEmpty(wufooFormHash)) {
            pageMeta.addMetadata("wufoo","formHash").addContent(wufooFormHash);
            Request request = ObjectModelHelper.getRequest(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);
            EPerson loggedin = context.getCurrentUser();
            String eperson = null;
            if (loggedin != null)
            {
                eperson = loggedin.getEmail();
            }
            String defaultValues = "";
            String joiner = "";
            if (StringUtils.isNotEmpty(configurationService.getProperty(PAGE_FIELD))) {
                String page = request.getHeader("Referer");
                // Remove scheme (Wufoo does not allow '//' in default values)
                page = page.substring(page.startsWith("https") ? 8 : 7);
                defaultValues += configurationService.getProperty(PAGE_FIELD) + "=" + page;
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(AGENT_FIELD))) {
                defaultValues += joiner + configurationService.getProperty(AGENT_FIELD) + "=" +
                        request.getHeader("User-Agent");
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(EMAIL_FIELD)) && eperson != null) {
                defaultValues += joiner + configurationService.getProperty(EMAIL_FIELD) + "=" + eperson;
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(EPERSON_FIELD))) {
                defaultValues += joiner + configurationService.getProperty(EPERSON_FIELD) + "=" + eperson;
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(SESSION_FIELD))) {
                defaultValues += joiner + configurationService.getProperty(SESSION_FIELD) + "=" +
                        request.getSession().getId();
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(DATE_FIELD))) {
                defaultValues += joiner + configurationService.getProperty(DATE_FIELD) + "=" + new Date();
                joiner = "&";
            }
            if (StringUtils.isNotEmpty(configurationService.getProperty(HOST_FIELD))) {
                defaultValues += joiner + configurationService.getProperty(HOST_FIELD) + "=" +
                        configurationService.getProperty("dspace.hostname");
            }
            pageMeta.addMetadata("wufoo","defaultValues").addContent(URLEncoder.encode(defaultValues, "UTF-8"));
        }
        // End Customization for LIBDRUM-563
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // Begin UMD Customization
        // Customization for LIBDRUM-563
        // No body needed for Wufoo feedback form
        String wufooFormHash = configurationService.getProperty(FORM_HASH);
        if (StringUtils.isNotEmpty(wufooFormHash)) {
            return;
        }
        // End Customization for LIBDRUM-563
        // Build the item viewer division.
        Division feedback = body.addInteractiveDivision("feedback-form",
                contextPath+"/feedback",Division.METHOD_POST,"primary");
        
        feedback.setHead(T_head);
        
        feedback.addPara(T_para1);
        
        List form = feedback.addList("form",List.TYPE_FORM);
        
        Text email = form.addItem().addText("email");
        email.setAutofocus("autofocus");
        email.setLabel(T_email);
        email.setHelp(T_email_help);
        email.setValue(parameters.getParameter("email",""));
        
        TextArea comments = form.addItem().addTextArea("comments");
        comments.setLabel(T_comments);
        comments.setValue(parameters.getParameter("comments",""));
        
        form.addItem().addButton("submit").setValue(T_submit);
        
        feedback.addHidden("page").setValue(parameters.getParameter("page","unknown"));
    }
}
