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
import org.dspace.authenticate.OrcidAuthentication;
import org.dspace.authenticate.OrcidAuthenticationBean;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.web.ContextUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class will filter ORCID requests and try and authenticate them.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */

public class OrcidLoginFilter extends StatelessLoginFilter {

    private static final Logger log = LogManager.getLogger(OrcidLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private OrcidAuthenticationBean orcidAuthentication = new DSpace().getServiceManager()
        .getServiceByName("orcidAuthentication", OrcidAuthenticationBean.class);

    public OrcidLoginFilter(String url, AuthenticationManager authenticationManager,
                                     RestAuthenticationService restAuthenticationService) {
        super(url, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
        throws AuthenticationException {

        if (!OrcidAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("Orcid login is disabled.");
        }

        return authenticationManager.authenticate(new DSpaceAuthentication());

    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
        Authentication auth) throws IOException, ServletException {


        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;

        log.debug("Orcid authentication successful for EPerson {}. Sending back temporary auth cookie",
            dSpaceAuthentication.getName());

        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, true);

        redirectAfterSuccess(req, res);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException failed) throws IOException, ServletException {

        Context context = ContextUtil.obtainContext(request);

        if (orcidAuthentication.isUsed(context, request)) {
            String baseRediredirectUrl = configurationService.getProperty("dspace.ui.url");
            String redirectUrl = baseRediredirectUrl + "/error?status=401&code=orcid.generic-error";
            response.sendRedirect(redirectUrl); // lgtm [java/unvalidated-url-redirection]
        } else {
            super.unsuccessfulAuthentication(request, response, failed);
        }

    }

    /**
     * After successful login, redirect to the DSpace URL specified by this Orcid
     * request (in the "redirectUrl" request parameter). If that 'redirectUrl' is
     * not valid or trusted for this DSpace site, then return a 400 error.
     * @param  request
     * @param  response
     * @throws IOException
     */
    private void redirectAfterSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get redirect URL from request parameter
        String redirectUrl = request.getParameter("redirectUrl");

        // If redirectUrl unspecified, default to the configured UI
        if (StringUtils.isEmpty(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }

        // Validate that the redirectURL matches either the server or UI hostname. It
        // *cannot* be an arbitrary URL.
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverHostName = Utils.getHostName(configurationService.getProperty("dspace.server.url"));
        ArrayList<String> allowedHostNames = new ArrayList<>();
        allowedHostNames.add(serverHostName);
        String[] allowedUrls = configurationService.getArrayProperty("rest.cors.allowed-origins");
        for (String url : allowedUrls) {
            allowedHostNames.add(Utils.getHostName(url));
        }

        if (StringUtils.equalsAnyIgnoreCase(redirectHostName, allowedHostNames.toArray(new String[0]))) {
            log.debug("Orcid redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("Invalid Orcid redirectURL=" + redirectUrl +
                ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Invalid redirectURL! Must match server or ui hostname.");
        }
    }

}
