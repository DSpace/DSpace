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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.core.Utils;
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
 * Rest controller that handles redirect after shibboleth authentication succeded
 *
 * @author Andrea Bollini (andrea dot bollini at 4science dot it)
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/shibboleth")
@RestController
public class ShibbolethRestController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ShibbolethRestController.class);

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

        // Validate that the redirectURL matches either the server or UI hostname. It *cannot* be an arbitrary URL.
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverHostName = Utils.getHostName(configurationService.getProperty("dspace.server.url"));
        String clientHostName = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
        if (StringUtils.equalsAnyIgnoreCase(redirectHostName, serverHostName, clientHostName)) {
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
