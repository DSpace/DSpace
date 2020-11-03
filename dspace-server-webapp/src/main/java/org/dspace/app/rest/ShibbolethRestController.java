/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles redirect *after* shibboleth authentication succeeded.
 * <P>
 * Shibboleth authentication does NOT occur in this Controller, but occurs before this class is called.
 * The general Shibboleth login process is as follows:
 *   1. When Shibboleth plugin is enabled, client/UI receives Shibboleth's absolute URL in WWW-Authenticate header.
 *      See {@link org.dspace.authenticate.ShibAuthentication} getLoginURL() method.
 *   2. Client sends the user to that URL when they select Shibboleth authentication.
 *   3. User logs in using Shibboleth
 *   4. If successful, they are redirected by Shibboleth to this Controller (the path of this controller is passed
 *      to Shibboleth as a URL param in step 1)
 *   5. NOTE: Prior to hitting this Controller, {@link org.dspace.app.rest.security.ShibbolethAuthenticationFilter}
 *      briefly intercepts the request in order to check for a valid Shibboleth login (see
 *      ShibAuthentication.authenticate()) and store that user info in a JWT.
 *   6. This Controller then gets the request & looks for a "redirectUrl" param (also a part of the original URL from
 *      step 1), and redirects the user to that location (after verifying its a trusted URL). Usually this is a redirect
 *      back to the Client/UI page where the User started.
 *
 * @author Andrea Bollini (andrea dot bollini at 4science dot it)
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @see org.dspace.authenticate.ShibAuthentication
 * @see org.dspace.app.rest.security.ShibbolethAuthenticationFilter
 */
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/shibboleth")
@RestController
public class ShibbolethRestController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ShibbolethRestController.class);

    @Autowired
    protected Utils utils;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/" + AuthnRest.CATEGORY, "shibboleth")));
    }

    // LGTM.com thinks this method has an unvalidated URL redirect (https://lgtm.com/rules/4840088/) in `redirectUrl`,
    // even though we are clearly validating the hostname of `redirectUrl` and test it in ShibbolethRestControllerIT
    @SuppressWarnings("lgtm[java/unvalidated-url-redirection]")
    @RequestMapping(method = RequestMethod.GET)
    public void shibboleth(HttpServletResponse response,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl) throws IOException {
        if (redirectUrl == null) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }

        // Redirect URL *cannot* be an arbitrary URL. Must be trusted.
        if (utils.isTrustedUrl(redirectUrl)) {
            log.debug("Shibboleth redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("Invalid Shibboleth redirectURL=" + redirectUrl +
                          ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid redirectURL! Must match server or ui hostname.");
        }
    }
}
