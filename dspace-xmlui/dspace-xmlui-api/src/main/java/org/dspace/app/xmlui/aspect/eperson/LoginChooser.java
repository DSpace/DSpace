/*
 * LoginChooser.java
 *
 * Version: $Revision: 1 $
 *
 * Date: $Date: 2007/02/07 11:08:42 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.eperson;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Session;
import org.apache.cocoon.environment.http.HttpEnvironment;
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
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Displays a list of authentication methods. This page is displayed if more
 * than one AuthenticationMethod is defined in the dpace config file.
 * 
 * @author Jay Paz
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
		Session session = request.getSession();
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
			// cacheable
			return "1";
		else
			// Uncachable
			return "0";
	}

	/**
	 * Generate the cache validity object.
	 */
	public SourceValidity getValidity() {
		Request request = ObjectModelHelper.getRequest(objectModel);
		String previous_email = request.getParameter("login_email");

		// Get any message parameters
		Session session = request.getSession();
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
			// Always valid
			return NOPValidity.SHARED_INSTANCE;
		else
			// invalid
			return null;
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
		Session session = request.getSession();

		// Get any message parameters
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		if (header != null || message != null || characters != null) {
			Division reason = body.addDivision("login-reason");

			if (header != null)
				reason.setHead(message(header));
			else
				// Allways have a head.
				reason.setHead("Authentication Required");

			if (message != null)
				reason.addPara(message(message));

			if (characters != null)
				reason.addPara(characters);
		}

		Division loginChooser = body.addDivision("login-chooser");
		loginChooser.setHead(T_head1);
		loginChooser.addPara().addContent(T_para1);

		List list = loginChooser.addList("login-options", List.TYPE_SIMPLE);

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

                final Item item = list.addItem();
                item.addXref(loginURL, message(authTitle));
            }

		}
	}

}
