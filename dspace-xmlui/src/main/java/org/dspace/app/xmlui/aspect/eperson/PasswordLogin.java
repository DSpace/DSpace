/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Password;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Query the user for their authentication credentials.
 * 
 * The parameter "return-url" may be passed to give a location 
 * where to redirect the user to after successfully authenticating.
 * 
 * @author Sid
 * @author Scott Phillips
 */
public class PasswordLogin extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /**language strings */
    public static final Message T_title =
    message("xmlui.EPerson.PasswordLogin.title");

    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    public static final Message T_trail =
        message("xmlui.EPerson.PasswordLogin.trail");
    
    public static final Message T_head1 =
        message("xmlui.EPerson.PasswordLogin.head1");
    
    public static final Message T_email_address =
        message("xmlui.EPerson.PasswordLogin.email_address");
    
    public static final Message T_error_bad_login = 
        message("xmlui.EPerson.PasswordLogin.error_bad_login");
    
    public static final Message T_password = 
        message("xmlui.EPerson.PasswordLogin.password");
    
    public static final Message T_forgot_link = 
        message("xmlui.EPerson.PasswordLogin.forgot_link");
    
    public static final Message T_submit = 
        message("xmlui.EPerson.PasswordLogin.submit");
    
    public static final Message T_head2 = 
        message("xmlui.EPerson.PasswordLogin.head2");
    
    public static final Message T_para1 =
        message("xmlui.EPerson.PasswordLogin.para1");
    
    public static final Message T_register_link = 
        message("xmlui.EPerson.PasswordLogin.register_link");
    
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String previous_email = request.getParameter("login_email");
        
        // Get any message parameters
        HttpSession session = request.getSession();
        String header = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
        String message = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
        String characters = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);
        
        
        // If there is a message or previous email attempt then the page is not cachable
        if (header == null && message == null && characters == null && previous_email == null)
        {
            // cacheable
            return "1";
        }
        else
        {
            // Uncachable
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String previous_email = request.getParameter("login_email");
        
        // Get any message parameters
        HttpSession session = request.getSession();
        String header = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
        String message = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
        String characters = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);
        
        
        // If there is a message or previous email attempt then the page is not cachable
        if (header == null && message == null && characters == null && previous_email == null)
        {
            // Always valid
            return NOPValidity.SHARED_INSTANCE;
        }
        else
        {
            // invalid
            return null;
        }
    } 
    
    
    /**
     * Set the page title and trail.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Display the login form.
     */
    public void addBody(Body body) throws SQLException, SAXException,
            WingException
    {
        // Check if the user has previously attempted to login.
        Request request = ObjectModelHelper.getRequest(objectModel);
        HttpSession session = request.getSession();
        String previousEmail = request.getParameter("login_email");
        
        // Get any message parameters
        String header = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
        String message = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
        String characters = (String) session.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);
        
        if (header != null || message != null || characters != null)
        {
        	Division reason = body.addDivision("login-reason");
        	
        	if (header != null)
            {
                reason.setHead(message(header));
            }
        	else
            {
                // Always have a head.
                reason.setHead("Authentication Required");
            }
        	
        	if (message != null)
            {
                reason.addPara(message(message));
            }
        	
        	if (characters != null)
            {
                reason.addPara(characters);
            }
        }
        
        
        Division login = body.addInteractiveDivision("login", contextPath
                + "/password-login", Division.METHOD_POST, "primary");
        login.setHead(T_head1);
        
        List list = login.addList("password-login",List.TYPE_FORM);

        Text email = list.addItem().addText("login_email");
        email.setAutofocus("autofocus");
        email.setRequired();
        email.setLabel(T_email_address);
        if (previousEmail != null)
        {
            email.setValue(previousEmail);
            email.addError(T_error_bad_login);
        }
        

        Item item = list.addItem();
        Password password = item.addPassword("login_password");
        password.setRequired();
        password.setLabel(T_password);
        item.addXref(contextPath + "/forgot", T_forgot_link);

        list.addLabel();
        Item submit = list.addItem("login-in", null);
        submit.addButton("submit").setValue(T_submit);
        
        if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.user.registration", true))
        {
	        Division register = login.addDivision("register");
	        register.setHead(T_head2);
	        register.addPara(T_para1);
	        register.addPara().addXref(contextPath + "/register",T_register_link);
        }
    }
}
