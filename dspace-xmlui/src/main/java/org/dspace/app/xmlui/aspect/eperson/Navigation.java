/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Locale;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Add the eperson navigation items to the document. This includes:
 * 
 * 1) Login and Logout links
 * 
 * 2) Navigational links to register or edit their profile based 
 *    upon whether the user is authenticatied or not.
 * 
 * 3) User metadata 
 * 
 * 4) The user's language preferences (whether someone is logged 
 *    in or not)
 * 
 * based on class by Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	/** log4j category */
	private static Logger log = LoggerFactory.getLogger(Navigation.class);

    /** Language Strings */
    private static final Message T_my_account =
        message("xmlui.EPerson.Navigation.my_account");
    
    private static final Message T_about_head =
            message("xmlui.EPerson.Navigation.about-head");
    private static final Message T_deposit =
            message("xmlui.EPerson.Navigation.deposit");
    private static final Message T_cite =
            message("xmlui.EPerson.Navigation.cite");
    private static final Message T_lifecycle =
            message("xmlui.EPerson.Navigation.item-lifecycle");
    private static final Message T_faq =
            message("xmlui.EPerson.Navigation.faq");
    private static final Message T_about =
            message("xmlui.EPerson.Navigation.about");
    
    private static final Message T_profile =
        message("xmlui.EPerson.Navigation.profile");
    
    private static final Message T_logout =
        message("xmlui.EPerson.Navigation.logout");
    
    //private static final Message T_login =
    //    message("xmlui.EPerson.Navigation.login");
    
    private static final Message T_register =
        message("xmlui.EPerson.Navigation.register");

    private static final Message T_helpdesk =
            message("xmlui.EPerson.Navigation.helpdesk");

    private static final Message T_discojuice_login =
        message("xmlui.EPerson.Navigation.discojuice.login");

    private static final Message T_context_head 				= message("xmlui.administrative.Navigation.context_head");

	/** Cached validity object */
	private SourceValidity validity;
	
    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey() 
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        // Special case, don't cache anything if the user is logging 
        // in. The problem occures because of timming, this cache key
        // is generated before we know whether the operation has 
        // succeeded or failed. So we don't know whether to cache this 
        // under the user's specific cache or under the anonymous user.
        if (request.getParameter("login_email")    != null ||
            request.getParameter("login_password") != null ||
            request.getParameter("login_realm")    != null )
        {
            return null;
        }
                
        // FIXME:
        // Do not cache the home page. There is a bug that is causing the
        // homepage to be cached with user's data after a logout. This
        // pollutes the cache. As a work-around this problem we just won't
        // cache this page.
        if (request.getSitemapURI().length() == 0)
        {
        	return null;
        }
        
    	StringBuilder key;
        if (context.getCurrentUser() != null)
        {
            key = new StringBuilder(context.getCurrentUser().getEmail());
        }
        else
        {
            key = new StringBuilder("anonymous");
        }
        
        // Add the user's language
        Enumeration locales = request.getLocales();
        while (locales.hasMoreElements())
        {
            Locale locale = (Locale) locales.nextElement();
            key.append("-").append(locale.toString());
        }
        
        return HashUtil.hash(key.toString());
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity() 
    {
    	if (this.validity == null)
    	{
    		// Only use the DSpaceValidity object is someone is logged in.
    		if (context.getCurrentUser() != null)
    		{
		        try {
		            DSpaceValidity validity = new DSpaceValidity();
		            
		            validity.add(eperson);
		            
		            Group[] groups = Group.allMemberGroups(context, eperson);
		            for (Group group : groups)
		            {
		            	validity.add(group);
		            }
		            
		            this.validity = validity.complete();
		        } 
		        catch (SQLException sqle)
		        {
		            // Just ignore it and return invalid.
		        }
    		}
    		else
    		{
    			this.validity = NOPValidity.SHARED_INSTANCE;
    		}
    	}
    	return this.validity;
    }
    
    /**
     * Add the eperson aspect navigational options.
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used 
    	 */
        options.addList("browse");
        List account = options.addList("account");
        List about = options.addList("about");
        List contextList = options.addList("context");
        options.addList("administrative");
        
        account.setHead(T_my_account);
        EPerson eperson = this.context.getCurrentUser();
        if (eperson != null)
        {
            String fullName = eperson.getFullName();
            account.addItemXref(contextPath+"/logout",T_logout);
            //account.addItemXref(contextPath+"/profile",T_profile.parameterize(fullName));
            account.addItemXref(contextPath+"/profile",T_profile);
        } 
        else 
        {
            // UFAL
            account.addItem().addXref(contextPath + "/login",
                    T_discojuice_login, "signon");

            final javax.servlet.http.HttpServletRequest hreq = (javax.servlet.http.HttpServletRequest) this.objectModel
                    .get(org.apache.cocoon.environment.http.HttpEnvironment.HTTP_REQUEST_OBJECT);

            String redirect = hreq.getPathInfo();
            if (redirect.endsWith(".continue"))
            {
                // Don't remember continuation addresses #896
                redirect = "login"; // See ShibbolethAction or
                                    // AuthenticateAction
            }
            hreq.getSession()
                    .setAttribute("xmlui.user.loginredirect", redirect);

            if (ConfigurationManager.getBooleanProperty(
                    "xmlui.user.registration", true))
            {
                account.addItemXref(contextPath + "/register", T_register);
            }
        }
        
        // about
        about.setHead( T_about_head );
        about.addItemXref( contextPath + "/page/deposit", T_deposit );
        about.addItemXref( contextPath + "/page/citate", T_cite);
        about.addItemXref( contextPath + "/page/item-lifecycle", T_lifecycle );
        about.addItemXref( contextPath + "/page/faq", T_faq);
        about.addItemXref( contextPath + "/page/about", T_about );
        about.addItem().addXref("mailto:" + ConfigurationManager.getProperty("lr.help.mail"),T_helpdesk, "helpdesk");
        
        //context
        // Context Administrative options
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	if (dso instanceof Item)
    	{
    		Item item = (Item) dso;
    		if (item.canEditMetadata() && !AuthorizeManager.isAdmin(context, item))
    		{
                    contextList.setHead(T_context_head);
                    contextList.addItem().addXref(contextPath+"/edit/item?itemID="+item.getID(), "Edit metadata");
            }
    	}

    }

    /**
     * Add the user metadata
     */
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        EPerson eperson = context.getCurrentUser();
        if (eperson != null)
        {
            userMeta.setAuthenticated(true);
            userMeta.addMetadata("identifier").addContent(eperson.getID());
            userMeta.addMetadata("identifier","email").addContent(eperson.getEmail());
            userMeta.addMetadata("identifier","firstName").addContent(eperson.getFirstName());
            userMeta.addMetadata("identifier","lastName").addContent(eperson.getLastName());
            userMeta.addMetadata("identifier","logoutURL").addContent(contextPath+"/logout");
            userMeta.addMetadata("identifier","url").addContent(contextPath+"/profile");
        }
        else
        {
            userMeta.setAuthenticated(false);
        }

        // Allways have a login URL.
// <UFAL>
//        userMeta.addMetadata("identifier","loginURL").addContent(contextPath+"/shibboleth-login");
        userMeta.addMetadata("identifier","loginURL").addContent(contextPath+"/login");
// </UFAL>
        
        // Always add language information
        Request request = ObjectModelHelper.getRequest(objectModel);
        Enumeration locales = request.getLocales();
        while (locales.hasMoreElements())
        {
            Locale locale = (Locale) locales.nextElement();
            userMeta.addMetadata("language","RFC3066").addContent(locale.toString());    
        }
    }
    
    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }
    
}
