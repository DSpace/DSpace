/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles redirect after OIDC authentication succeded.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RestController
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/oidc")
public class OidcRestController {

    private static final Logger log = LoggerFactory.getLogger(OidcRestController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @PostConstruct
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this, List.of(Link.of("/api/" + AuthnRest.CATEGORY, "oidc")));
    }

    @RequestMapping(method = RequestMethod.GET)
    public void oidc(HttpServletResponse response,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl) throws IOException {
        if (StringUtils.isBlank(redirectUrl)) {
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
            log.debug("OIDC redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl); // lgtm [java/unvalidated-url-redirection]
        } else {
            log.error("Invalid OIDC redirectURL=" + redirectUrl +
                          ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid redirectURL! Must match server or ui hostname.");
        }

    }
}
