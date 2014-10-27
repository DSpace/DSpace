/*
 * PasswordLogin.java
 * 
 * Version: $Revision: 3705 $
 * 
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 * 
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts Institute
 * of Technology. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.usagelogging.EventLogger;
import org.xml.sax.SAXException;

/**
 * Query the user for their authentication credentials.
 * 
 * The parameter "return-url" may be passed to give a location where to redirect
 * the user to after successfully authenticating.
 * 
 * @author Sid
 * @author Scott Phillips
 * @author Kevin Clarke
 */
public class PasswordLogin extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {
	/** language strings */
	public static final Message T_title = message("xmlui.EPerson.PasswordLogin.title");

	public static final Message T_dspace_home = message("xmlui.general.dspace_home");

	public static final Message T_trail = message("xmlui.EPerson.PasswordLogin.trail");

	public static final Message T_head1 = message("xmlui.EPerson.PasswordLogin.head1");
	
	public static final Message T_reglogin_head = message("xmlui.EPerson.RegLogin.head");

	public static final Message T_email_address = message("xmlui.EPerson.PasswordLogin.email_address");

	public static final Message T_error_bad_login = message("xmlui.EPerson.PasswordLogin.error_bad_login");

	public static final Message T_password = message("xmlui.EPerson.PasswordLogin.password");

	public static final Message T_forgot_link = message("xmlui.EPerson.PasswordLogin.forgot_link");

	public static final Message T_submit_register = message("xmlui.EPerson.PasswordLogin.submitReg");

	public static final Message T_submit_login = message("xmlui.EPerson.PasswordLogin.submitLogin");

	public static final Message T_head2 = message("xmlui.EPerson.PasswordLogin.head2");

	public static final Message T_para1 = message("xmlui.EPerson.PasswordLogin.para1");

	public static final Message T_register_link = message("xmlui.EPerson.PasswordLogin.register_link");

	private static final Message T_email_address_help = message("xmlui.EPerson.StartRegistration.email_address_help");

    private static final Message T_no_ocrid_found = message("xmlui.EPerson.StartRegistration.no_orcid_found");
	/** The email address previously entered */
	private String email;

	/**
	 * Determine if the user failed on their last attempt to enter an email
	 * address
	 */
	private java.util.List<String> errors;

	/**
	 * Determine if the last failed attempt was because an account allready
	 * existed for the given email address
	 */
	private boolean accountExists;

	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,
			IOException {
		super.setup(resolver, objectModel, src, parameters);

		this.email = parameters.getParameter("email", "");
		this.accountExists = parameters.getParameterAsBoolean("accountExists",
				false);
		String errors = parameters.getParameter("errors", "");
		if (errors.length() > 0)
			this.errors = Arrays.asList(errors.split(","));
		else
			this.errors = new ArrayList<String>();
	}

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
		// cacheable
		if (header == null && message == null && characters == null
				&& previous_email == null && accountExists == false
				&& errors != null && errors.size() == 0)
			// cacheable
			return "1";
		else
			// Uncacheable
			return "0";
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
		// cacheable
		if (header == null && message == null && characters == null
				&& previous_email == null && errors != null
				&& errors.size() == 0 && accountExists == false)
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
	 * Display the login form.
	 */
	public void addBody(Body body) throws SQLException, SAXException,
			WingException {
		// Check if the user has previously attempted to login.
		Request request = ObjectModelHelper.getRequest(objectModel);
		HttpSession session = request.getSession();
		String previousEmail = request.getParameter("login_email");
		boolean accountExists = parameters.getParameterAsBoolean(
				"accountExists", false);

		if (previousEmail != null) {
			email = previousEmail;
		}
		else {
			previousEmail = request.getParameter("email");
		}
		
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
				// Always have a head.
				reason.setHead("Authentication Required");

			if (message != null)
				reason.addPara(message(message));

			if (characters != null)
				reason.addPara(characters);
		}

        if(request.getParameter("exist_orcid")!=null){
            Division orcidDiv = body.addDivision("orcid-error");
            orcidDiv.addPara("There is already a eperson linked to this orcid id:");
            orcidDiv.addPara(request.getParameter("exist_orcid"));
        }
        if(request.getParameter("set_orcid")!=null){
            Division orcidDiv = body.addDivision("orcid-error");
            orcidDiv.addPara(T_no_ocrid_found);
        }

		Division register = body.addInteractiveDivision("register",
				contextPath + "/register", Division.METHOD_POST,
				"register-left");

		register.addPara(T_reglogin_head);

                List regList = register.addList("register", List.TYPE_FORM);
		regList.setHead(T_head2);
                Item regEmailItem = regList.addItem("regemail-item", "register-row");
                Text regEmail = regEmailItem.addText("email", "register-email");
		regEmail.setRequired();
		regEmail.setLabel(T_email_address);
		if (previousEmail != null) {
			regEmail.setValue(previousEmail);
			regEmail.addError(T_error_bad_login);
		}
                Item emptyItem = regList.addItem("empty-item", "register-row");
		if (knot != null) {
			emptyItem.addHidden("eperson-continue").setValue(knot.getId());
		}
                Item submitItem = regList.addItem("submit-item", "register-row");
                Button submitButton = submitItem.addButton("submit");
                submitButton.setValue(T_submit_register);

                /* login */

		Division login = body.addInteractiveDivision("login", contextPath
				+ "/password-login", Division.METHOD_POST, "login-right");

		List loginList = login.addList("login", List.TYPE_FORM);
                loginList.setHead(T_head1);

                Item loginEmailItem = loginList.addItem("loginemail-item", "login-row");
                Text loginEmail = loginEmailItem.addText("login_email", "login-email");
		loginEmail.setRequired();
		loginEmail.setLabel(T_email_address);
		if (previousEmail != null) {
			loginEmail.setValue(previousEmail);
			loginEmail.addError(T_error_bad_login);
		}

                Item loginPasswordItem = loginList.addItem("loginpassword-item", "login-row");
		Password loginPassword = loginPasswordItem.addPassword("login_password");
                loginPassword.setRequired();
                loginPassword.setLabel(T_password);

                Item loginSubmitItem = loginList.addItem("loginsubmit-item", "login-row");
                Button loginSubmitButton = loginSubmitItem.addButton("submit");
                loginSubmitButton.setValue(T_submit_login);
        Para p=login.addPara();

        p.addXref("/oauth-login","Oauth login");
                p.addXref("/forgot", T_forgot_link);
                EventLogger.log(context, "login-form", "previous-email=" + (previousEmail != null ? previousEmail : ""));

	}

	// HttpServletRequest  httpRequest  = (HttpServletRequest)  objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
	
	/**
	 * Recycle
	 */
	public void recycle() {
		this.email = null;
		this.errors = null;
		super.recycle();
	}
}
