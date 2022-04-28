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
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles redirect after ORCID authentication succeded.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RestController
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY + "/orcid")
public class OrcidAuthenticationRestController {

    private static final Logger log = LoggerFactory.getLogger(OrcidAuthenticationRestController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @PostConstruct
    public void postConstruct() {
        discoverableEndpointsService.register(this, Arrays.asList(Link.of("/api/" + AuthnRest.CATEGORY, "orcid")));
    }

    @GetMapping
    public void orcid(HttpServletResponse response,
                      @RequestParam(name = "redirectUrl", required = false) String redirectUrl) throws IOException {
        if (StringUtils.isBlank(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }
        log.info("Redirecting to " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
