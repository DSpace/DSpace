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
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.dspace.authenticate.ShibAuthentication;

/**
 * Displays a list of authentication methods. This page is displayed if more
 * than one AuthenticationMethod is defined in the dpace config file.
 * 
 * based on class by Jay Paz
 * modified for LINDAT/CLARIN
 * 
 */
public class LoginChooser extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	public static final Message T_dspace_home = message("xmlui.general.dspace_home");

	public static final Message T_title = message("xmlui.EPerson.LoginChooser.title");

	public static final Message T_trail = message("xmlui.EPerson.LoginChooser.trail");

	public static final Message T_head1 = message("xmlui.EPerson.LoginChooser.head1");

	public static final Message T_para1 = message("xmlui.EPerson.LoginChooser.para1");

	/**
	 * Generate the unique caching key. This key must be unique inside the space
	 * of this component.
	 */
	public Serializable getKey() {
		Request request = ObjectModelHelper.getRequest(objectModel);
		String previous_email = request.getParameter("login_email");

		// Get any message parameters
		HttpSession session = request.getSession();
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		// If there is a message or previous email attempt then the page is not
		// cachable
		if (header == null && message == null && characters == null
				&& previous_email == null)
        {
            // cacheable

			// never cache, there have been few problems with redirects?!
			return "0";
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
	public SourceValidity getValidity() {
		Request request = ObjectModelHelper.getRequest(objectModel);
		String previous_email = request.getParameter("login_email");

		// Get any message parameters
		HttpSession session = request.getSession();
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		// If there is a message or previous email attempt then the page is not
		// cachable
		if (header == null && message == null && characters == null
				&& previous_email == null)
		{
			// invalid
			return null;
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
	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	/**
	 * Display the login choices.
	 */
	public void addBody(Body body) throws SQLException, SAXException,
			WingException {
		Iterator authMethods = AuthenticationManager
				.authenticationMethodIterator();
		Request request = ObjectModelHelper.getRequest(objectModel);
		HttpSession session = request.getSession();

		// Get any message parameters
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);
		boolean embargo_err = false;
		if ( (header != null && header.trim().length() > 0) || 
			 (message != null && message.trim().length() > 0) ||
			 (characters != null && characters.trim().length() > 0)) {
			Division reason = body.addDivision("login-reason",
					"alert alert-error");

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
				// specific handling of embargo - see
				// BitstreamReader.set_authorised_error
				if (characters.startsWith("embargo:")) {
					List l = reason.addList("embargo-info", List.TYPE_FORM,
							"embargo-info");
					Item i = l.addItem(null, "label label-important");
					i.addContent("Available after (year-month-day): ");
					i.addContent(characters.split("embargo:")[1]);					
					embargo_err = true;
					l.addItem(null, "fa fa-clock-o fa-5x hangright").addContent(" ");
				}
				else
				{
					reason.addPara(characters);
				}

			}
		}

		if (!embargo_err)
		{
			Division loginChooser = body.addDivision("login-chooser", "alert alert-error");
			loginChooser.setHead(T_head1);
			List list = loginChooser.addList("login-options", List.TYPE_FORM);
			list.addItem().addContent("You are trying to access a restricted resource / page.\nPlease choose a login method below to authenticate.");
			// list.addItem().addFigure("./themes/UFAL/images/avatar.jpg", "#",
			// "login-options-avatar signon");
			//Item item = list.addItem("please-login", "explicit-login-form");
			// item.addContent("Please ");
			//item.addXref("#", "Unified Login", "login-page-anchor signon");


		while (authMethods.hasNext()) {
			final AuthenticationMethod authMethod = (AuthenticationMethod) authMethods
					.next();

            HttpServletRequest hreq = (HttpServletRequest) this.objectModel
                    .get(HttpEnvironment.HTTP_REQUEST_OBJECT);

            HttpServletResponse hresp = (HttpServletResponse) this.objectModel
                    .get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            
            String loginURL = authMethod.loginPageURL(context, hreq, hresp);

            String authTitle = authMethod.loginPageTitle(context);

            if (loginURL != null && authTitle != null)
            {

                if (ConfigurationManager.getBooleanProperty("xmlui.force.ssl")
                        && !request.isSecure())
                {
                    StringBuffer location = new StringBuffer("https://");
                    location
                            .append(
                                    ConfigurationManager
                                            .getProperty("dspace.hostname"))
                            .append(loginURL).append(
                                    request.getQueryString() == null ? ""
                                            : ("?" + request.getQueryString()));
                    loginURL = location.toString();
                }
					if (ShibAuthentication.class.getName().equals(authMethod.getClass().getName())) {
						Item item = list.addItem();
						item.addXref("#", message("Unified Login"), "signon label label-important");
						//item.addFigure("./themes/UFAL/images/discojuice-logo.png", "#", "signon"); // comment to the link
						item = list.addItem();
						//Message discoComment = message("xmlui.EPerson.LoginChooser.discojuiceComment");
						//item.addContent(discoComment);
	
						// Eduid link item = list.addItem();
						//item.addFigure("./themes/UFAL/images/eduid-logo.png", loginURL, null);
						//item.addXref(loginURL, "eduID.cz Login", "label label-important");
						//final Message eduidComment = message("xmlui.EPerson.LoginChooser.eduidComment");
						//item.addContent(eduidComment);
					} else { // Normal Render
						//final Item item = list.addItem();
						//final Message otherwiseComment = message("xmlui.EPerson.LoginChooser.otherwiseComment");
						// XXX not flexible if more auth methods
						//item.addContent(otherwiseComment);
						//item.addXref(loginURL, message(authTitle));
					}
				}

            }

			list.addItem(null, "fa fa-lock fa-5x hangright").addContent(" ");
		
		}
	}

}
