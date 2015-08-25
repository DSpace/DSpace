package org.dspace.app.webui.servlet;

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authority.orcid.OrcidAccessToken;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;

/**
 * Attempt to authenticate the user based upon their ORCID OAuth Permissions.
 * 
 * If the authentication attempt is successfull then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the system
 * before authenticated or if none is supplied back to the DSpace homepage. The
 * action will also return true, thus contents of the action will be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:match pattern="oauth-login"> <map:act type="OAuthAuthenticateAction"> <!
 * -- Loggin succeeded, request will be forwarded. -->
 * <map:serialize type="xml"/> </map:act> <!-- Login failed, try again. Show
 * them static content from xml file -->
 * <map:transform type="FailedAuthentication" /> <map:serialize type="xml"/>
 * </map:match>
 *
 * @author Mark Diggory, Lantian Gai
 */

public class OAuthAuthenticationServlet extends DSpaceServlet {

	private static final Logger log = Logger.getLogger(OAuthAuthenticationServlet.class);

	/**
	 * Attempt to authenticate the user.
	 */
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
			throws javax.servlet.ServletException, IOException, SQLException, org.dspace.authorize.AuthorizeException {
		doDSPost(context, request, response);

	}

	@Override
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		try {
			/*
			 * Implementing and OAUth Flow Goals: 1. Redirect User to specific
			 * OAuth Service to approve access. 2. Process returning OAuth
			 * request and Generate Token
			 */

			// 1. Check for requirements for next OAuth FLow Step, is the client
			// returning from the OAuth server or is
			// this an initial entry into the service.

			OAuthAuthzResponse oar = null;

			try {
				oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
			} catch (Exception e) {
				// NOTHING
			}

			if (oar == null || oar.getCode() == null) {
				// Step 1. there is no code and we need to request one.
				AuthenticationRequestBuilder builder = OAuthClientRequest
						.authorizationLocation(
								ConfigurationManager.getProperty("authentication-oauth", "application-authorize-url"))
						.setClientId(ConfigurationManager.getProperty("authentication-oauth", "application-client-id"))
						.setRedirectURI(
								ConfigurationManager.getProperty("authentication-oauth", "application-redirect-uri"))
						.setResponseType("code")
						.setScope(ConfigurationManager.getProperty("authentication-oauth", "application-client-scope"));

				String showLogin = request.getParameter("show-login");
				if (StringUtils.isNotBlank(showLogin)) {
					boolean showLoginB = Boolean.parseBoolean(showLogin);
					builder.setParameter("email", context.getCurrentUser().getEmail());
					if (showLoginB) {
						builder.setParameter("show_login", "true");
					} else {
						builder.setParameter("family_names", context.getCurrentUser().getLastName());
						builder.setParameter("given_names", context.getCurrentUser().getFirstName());
					}
				}
				OAuthClientRequest oAuthClientRequest = builder.buildQueryMessage();

				// Issue a Redirect to the OAuth site to request authorization
				// code.

				response.sendRedirect(oAuthClientRequest.getLocationUri());
				return;
			} else if (oar != null && oar.getCode() != null) {
				// Step 2. Retrieve the oar and attempt to get a fresh token.

				// Step 2.a. User the new Code to Get An Access Token
				OAuthClientRequest oAuthClientRequest = OAuthClientRequest
						.tokenLocation(
								ConfigurationManager.getProperty("authentication-oauth", "application-token-url"))
						.setGrantType(GrantType.AUTHORIZATION_CODE)
						.setClientId(ConfigurationManager.getProperty("authentication-oauth", "application-client-id"))
						.setClientSecret(
								ConfigurationManager.getProperty("authentication-oauth", "application-client-secret"))
						.setRedirectURI(
								ConfigurationManager.getProperty("authentication-oauth", "application-redirect-uri"))
						.setCode(oar.getCode()).buildQueryMessage();

				// create OAuth client that uses custom http client under the
				// hood
				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
				try {
					OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(oAuthClientRequest,
							OAuthJSONAccessTokenResponse.class);

					// Step 2.b. Retrieve the access and expiration Tokens.
					request.setAttribute("orcid", oAuthResponse.getParam("orcid"));
					request.setAttribute("access_token", oAuthResponse.getAccessToken());
					request.setAttribute("expires_in", oAuthResponse.getExpiresIn());
					request.setAttribute("token_type", oAuthResponse.getParam("token_type"));
					request.setAttribute("scope", oAuthResponse.getScope());
					request.setAttribute("refresh_token", oAuthResponse.getRefreshToken());
					request.setAttribute("oauthResponse", oAuthResponse);
					log.info("Retrieved oauthResponse from apache.oltu");
				} catch (Exception ex) {
					//WARN in particular condition the accesstoken method fails and we can retrieve the access token using jersey directly
					OrcidService orcidService = OrcidService.getOrcid();
					OrcidAccessToken oAuthResponse = orcidService.getAuthorizationAccessToken(oar.getCode());

					request.setAttribute("orcid", oAuthResponse.getOrcid());
					request.setAttribute("access_token", oAuthResponse.getAccess_token());
					request.setAttribute("expires_in", oAuthResponse.getExpires_in());
					request.setAttribute("token_type", oAuthResponse.getToken_type());
					request.setAttribute("scope", oAuthResponse.getScope());
					request.setAttribute("refresh_token", oAuthResponse.getRefresh_token());
					request.setAttribute("oauthResponse", oAuthResponse);
					log.info("Retrieved oauthResponse from jersey");
				}
			}
		} catch (Exception e) {
			throw new ServletException("Unable to preform authentication: " + e.getMessage(), e);
		}

		// Locate the eperson
		int status = AuthenticationManager.authenticate(context, null, null, null, request);

		String jsp = null;
		if (status == AuthenticationMethod.SUCCESS) {
			// Logged in OK.
			Authenticate.loggedIn(context, request, context.getCurrentUser());

			// Set the Locale according to user preferences
			Locale epersonLocale = I18nUtil.getEPersonLocale(context.getCurrentUser());
			context.setCurrentLocale(epersonLocale);
			Config.set(request.getSession(), Config.FMT_LOCALE, epersonLocale);

			log.info(LogManager.getHeader(context, "login", "type=orcid"));

			// resume previous request
			Authenticate.resumeInterruptedRequest(request, response);

			return;
		} else if (status == AuthenticationMethod.CERT_REQUIRED) {
			jsp = "/error/require-certificate.jsp";
		} else if (status == AuthenticationMethod.NO_SUCH_USER) {
			log.info(LogManager.getHeader(context, "failed_login", "type=orcid, no_such_user"));
			jsp = "/login/orcid-not-in-records.jsp";
		} else {
			log.info(LogManager.getHeader(context, "failed_login", "type=orcid, oauth authentication error"));
			jsp = "/login/orcid-incorrect.jsp";
		}
		JSPManager.showJSP(request, response, jsp);
	}
}
