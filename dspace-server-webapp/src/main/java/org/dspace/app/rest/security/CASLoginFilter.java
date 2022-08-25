/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * UMD-custom class for handling CAS authentication. This class is based on
 * the org.dspace.app.rest.security.ShibbolethLoginFilter class.
 *
 * This class is in the "dspace-server-webapp" module, because there does not
 * appear to be a Spring mechanism for overriding the
 * org.dspace.app.rest.security.WebSecurityConfiguration` class in which the
 * login filter classes are configured.
 *
 * See "dspace/docs/CASAuthentication.md" for more information about CAS
 * authentication.
 */
public class CASLoginFilter extends StatelessLoginFilter {
    private static final Logger log = LogManager.getLogger(CASLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public CASLoginFilter(String url, AuthenticationManager authenticationManager,
                          RestAuthenticationService restAuthenticationService) {
        super(url, authenticationManager, restAuthenticationService);
        logger.info("Created CASLoginFilter");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        // Commenting out this check, because otherwise the CASAuthentication
        // class would need to be in this module (or a parent module), instead
        // of in the "additions" module.
        /*
        if (!CASAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("CAS is disabled.");
        }
        */

        // In the case of CAS, this method does NOT actually authenticate us
        // (the authentication has already happened CAS). So, this call to
        // "authenticate()" is just triggering/ CASAuthentication.authenticate()
        // to check for a valid CAS login, and if found, the current user
        // is considered authenticated via CAS.
        //
        // NOTE: because this authentication is implicit, we pass in an empty DSpaceAuthentication
        return authenticationManager.authenticate(new DSpaceAuthentication());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        // Once we've gotten here, we know we have a successful login
        // (i.e. attemptAuthentication() succeeded)

        // This method is using the same logic/mechanisms as
        // org.dspace.app.rest.security.ShibbolethLoginFilter for handling
        // the JWT.

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        log.debug("CAS authentication successful for EPerson {}. Sending back temporary auth cookie",
                  dSpaceAuthentication.getName());

        // OVERRIDE DEFAULT behavior of StatelessLoginFilter to return a temporary authentication cookie containing
        // the Auth Token (JWT). This Cookie is required because we *redirect* the user back to the client/UI after
        // a successful Shibboleth login. Headers cannot be sent via a redirect, so a Cookie must be sent to provide
        // the auth token to the client. On the next request from the client, the cookie is read and destroyed & the
        // Auth token is only used in the Header from that point forward.
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, true);

        // redirect user after completing CAS authentication, sending along the temporary auth cookie
        redirectAfterSuccess(req, res);
    }


    /**
     * After successful login, redirect to the DSpace URL specified by this CAS
     * request (in the "redirectUrl" request parameter). If that 'redirectUrl'
     * is not valid or trusted for this DSpace site, then return a 400 error.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    private void redirectAfterSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // This method is using the same logic/mechanisms as
        // org.dspace.app.rest.security.ShibbolethLoginFilter
        String redirectUrl = request.getParameter("redirectUrl");

        // If redirectUrl unspecified, default to the configured UI
        if (StringUtils.isEmpty(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }

        // Validate that the redirectURL matches either the server or UI hostname. It *cannot* be an arbitrary URL.
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverHostName = Utils.getHostName(configurationService.getProperty("dspace.server.url"));
        ArrayList<String> allowedHostNames = new ArrayList<>();
        allowedHostNames.add(serverHostName);
        String[] allowedUrls = configurationService.getArrayProperty("rest.cors.allowed-origins");
        for (String url : allowedUrls) {
            allowedHostNames.add(Utils.getHostName(url));
        }

        if (StringUtils.equalsAnyIgnoreCase(redirectHostName, allowedHostNames.toArray(new String[0]))) {
            log.debug("CAS redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("Invalid CAS redirectURL=" + redirectUrl +
                          ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid redirectURL! Must match server or ui hostname.");
        }
    }
}
