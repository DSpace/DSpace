/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.authenticate.OrcidAuthenticationBean.ORCID_AUTH_ATTRIBUTE;
import static org.dspace.authenticate.OrcidAuthenticationBean.ORCID_DEFAULT_REGISTRATION_URL;
import static org.dspace.authenticate.OrcidAuthenticationBean.ORCID_REGISTRATION_TOKEN;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * In this case, the actual authentication is performed by ORCID. After authentication succeeds, ORCID will send
 * the authentication data to this filter in order for it to be processed by DSpace.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */

public class OrcidLoginFilter extends StatelessLoginFilter {

    private static final Logger log = LogManager.getLogger(OrcidLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private OrcidAuthenticationBean orcidAuthentication = new DSpace().getServiceManager()
                                                                      .getServiceByName("orcidAuthentication",
                                                                                        OrcidAuthenticationBean.class);

    public OrcidLoginFilter(String url, String httpMethod, AuthenticationManager authenticationManager,
                                     RestAuthenticationService restAuthenticationService) {
        super(url, httpMethod, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
        throws AuthenticationException {

        if (!OrcidAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("Orcid login is disabled.");
        }
        // NOTE: because this authentication is implicit, we pass in an empty DSpaceAuthentication
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

        if (!orcidAuthentication.isUsed(context, request)) {
            super.unsuccessfulAuthentication(request, response, failed);
            return;
        }

        String baseRediredirectUrl = configurationService.getProperty("dspace.ui.url");
        String redirectUrl = baseRediredirectUrl + "/error?status=401&code=orcid.generic-error";
        Object registrationToken = request.getAttribute(ORCID_REGISTRATION_TOKEN);
        if (registrationToken != null) {
            final String orcidRegistrationDataUrl =
                configurationService.getProperty("orcid.registration-data.url", ORCID_DEFAULT_REGISTRATION_URL);
            redirectUrl = baseRediredirectUrl + MessageFormat.format(orcidRegistrationDataUrl, registrationToken);
            if (log.isDebugEnabled()) {
                log.debug(
                    "Orcid authentication failed for user with ORCID {}.",
                    request.getAttribute(ORCID_AUTH_ATTRIBUTE)
                );
                log.debug("Redirecting to {} for registration completion.", redirectUrl);
            }
        }

        response.sendRedirect(redirectUrl); // lgtm [java/unvalidated-url-redirection]
    }

    /**
     * After successful login, redirect to the DSpace URL specified by this Orcid
     * request (in the "redirectUrl" request parameter). If that 'redirectUrl' is
     * not valid or trusted for this DSpace site, then return a 400 error.
     *
     * @param request
     * @param response
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
