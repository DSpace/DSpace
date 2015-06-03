/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.eperson;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Attempt to authenticate the user based upon their ORCID OAuth Permissions.
 * 
 * If the authentication attempt is successfull then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the 
 * system before authenticated or if none is supplied back to the DSpace 
 * homepage. The action will also return true, thus contents of the action will
 * be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:match pattern="oauth-login">
 *     <map:act type="OAuthAuthenticateAction">
 *      <!-- Loggin succeeded, request will be forwarded. -->
 *          <map:serialize type="xml"/>
 *     </map:act>
 *     <!-- Login failed, try again. Show them static content from xml file -->
 *     <map:transform type="FailedAuthentication" />
 *     <map:serialize type="xml"/>
 * </map:match>
 *
 * @author Mark Diggory, Lantian Gai
 */
public class OAuthAuthenticateAction extends AbstractAction
{

    private static final Logger log = Logger.getLogger(OAuthAuthenticateAction.class);

    /**
     * Attempt to authenticate the user. 
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws PatternException {

        // First check if we are preforming a new login
        Request request = ObjectModelHelper.getRequest(objectModel);
        Response response = ObjectModelHelper.getResponse(objectModel);

        HttpServletResponse response1 =  (HttpServletResponse)objectModel.get("httpresponse");

        try
        {
            /* Implementing and OAUth Flow
               Goals:
                1. Redirect User to specific OAuth Service to approve access.
                2. Process returning OAuth request and Generate Token
             */

            // 1. Check for requirements for next OAuth FLow Step, is the client returning from the OAuth server or is
            // this an initial entry into the service.

            OAuthAuthzResponse oar = null;

            try{
                oar = OAuthAuthzResponse.oauthCodeAuthzResponse((HttpServletRequest)objectModel.get("httprequest"));
            }
            catch(Exception e)
            {}

            boolean test = "true".equals(request.getParameter("test"));

            if(!test)
            {
                if(oar == null || oar.getCode() == null)
                {
                    // Step 1. there is no code and we need to request one.
                    OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                            .authorizationLocation(ConfigurationManager.getProperty("authentication-oauth", "application-authorize-url"))
                            .setClientId(ConfigurationManager.getProperty("authentication-oauth", "application-client-id"))
                            .setRedirectURI(ConfigurationManager.getProperty("authentication-oauth", "application-redirect-uri"))
                            .setResponseType("code")
                            .setScope(ConfigurationManager.getProperty("authentication-oauth","application-client-scope"))
                            .buildQueryMessage();

                    // Issue a Redirect to the OAuth site to request authorization code.
                    response1.sendRedirect(oAuthClientRequest.getLocationUri());
                    return null;
                }
                else if(oar != null && oar.getCode() != null)
                {
                    // Step 2. Retrieve the oar and attempt to get a fresh token.

                    // Step 2.a. User the new Code to Get An Access Token
                    OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                            .tokenLocation(ConfigurationManager.getProperty("authentication-oauth", "application-token-url"))
                            .setGrantType(GrantType.AUTHORIZATION_CODE)
                            .setClientId(ConfigurationManager.getProperty("authentication-oauth", "application-client-id"))
                            .setClientSecret(ConfigurationManager.getProperty("authentication-oauth", "application-client-secret"))
                            .setRedirectURI(ConfigurationManager.getProperty("authentication-oauth", "application-redirect-uri"))
                            .setCode(oar.getCode())
                            .buildQueryMessage();

                    //create OAuth client that uses custom http client under the hood
                    OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

                    OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(oAuthClientRequest, OAuthJSONAccessTokenResponse.class);

                    // Step 2.b. Retrieve the access and expiration Tokens.
                    request.setAttribute("orcid",oAuthResponse.getParam("orcid"));
                    request.setAttribute("access_token",oAuthResponse.getAccessToken());
                    request.setAttribute("expires_in",oAuthResponse.getExpiresIn());
                    request.setAttribute("token_type",oAuthResponse.getParam("token_type"));
                    request.setAttribute("scope",oAuthResponse.getScope());
                    request.setAttribute("refresh_token",oAuthResponse.getRefreshToken());
                    request.setAttribute("oauthResponse",oAuthResponse);

                }
            }



            if(test){
                request.setAttribute("orcid",request.getParameter("orcid"));
                request.setAttribute("access_token","1");
                request.setAttribute("expires_in","2");
                request.setAttribute("token_type","3");
                request.setAttribute("scope","4");
                request.setAttribute("refresh_token","5");
            }

        } catch (Exception e) {
            throw new PatternException("Unable to preform authentication: " + e.getMessage(), e);
        }


        try
        {
            Context context = AuthenticationUtil.authenticate(objectModel, null, null, null);
            EPerson eperson = context.getCurrentUser();
            String orcidStatue= "";

            if(request.getSession().getAttribute("exist_orcid")!=null)
            {
                orcidStatue="?exist_orcid="+request.getSession().getAttribute("exist_orcid");
            }
            if(request.getSession().getAttribute("set_orcid")!=null)
            {
                orcidStatue="?set_orcid="+request.getSession().getAttribute("set_orcid");
            }


            if(orcidStatue.length()>0)
            {
                String redirectLink = "/password-login";
                if(eperson!=null)
                {
                    redirectLink = "/profile";
                }
                // Authentication failed send a redirect.
                final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

                httpResponse.sendRedirect(redirectLink+orcidStatue);

                return null;
            }
            else {
                if (eperson != null)
                {
                    // The user has successfully logged in
                    String redirectURL = request.getContextPath();

                    if (AuthenticationUtil.isInterupptedRequest(objectModel))
                    {
                        // Resume the request and set the redirect target URL to
                        // that of the originaly interrupted request.
                        redirectURL += AuthenticationUtil.resumeInterruptedRequest(objectModel);
                    }
                    else
                    {
                        // Otherwise direct the user to the login page
                        String loginRedirect = "/profile";
                        redirectURL += (loginRedirect != null) ? loginRedirect.trim() : "";
                    }

                    // Authentication successfull send a redirect.
                    final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

                    httpResponse.sendRedirect(redirectURL);

                    // log the user out for the rest of this current request, however they will be reauthenticated
                    // fully when they come back from the redirect. This prevents caching problems where part of the
                    // request is preformed fore the user was authenticated and the other half after it succedded. This
                    // way the user is fully authenticated from the start of the request.
                    context.setCurrentUser(null);

                    return new HashMap();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PatternException("Unable to preform authentication: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PatternException("Unable to preform authentication: " + e.getMessage(), e);
        }

        return null;
    }

}
