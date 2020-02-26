/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.AuthenticationStatusRest;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.hateoas.AuthenticationStatusResource;
import org.dspace.app.rest.model.hateoas.AuthnResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles authentication on the REST API together with the Spring Security filters
 * configured in {@link org.dspace.app.rest.security.WebSecurityConfiguration}
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY)
@RestController
public class AuthenticationRestController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationRestController.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ConverterService converter;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @Autowired
    private Utils utils;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/" + AuthnRest.CATEGORY, AuthnRest.NAME)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public AuthnResource authn() {
        AuthnRest authnRest = new AuthnRest();
        authnRest.setProjection(utils.obtainProjection());
        return converter.toResource(authnRest);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public AuthenticationStatusResource status(HttpServletRequest request, HttpServletResponse response)
            throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPersonRest ePersonRest = null;
        Projection projection = utils.obtainProjection();
        if (context.getCurrentUser() != null) {
            ePersonRest = converter.toRest(context.getCurrentUser(), projection);
        }

        AuthenticationStatusRest authenticationStatusRest = new AuthenticationStatusRest(ePersonRest);
        // Whether authentication status is false add WWW-Authenticate so client can retrieve the available
        // authentication methods
        if (!authenticationStatusRest.isAuthenticated()) {
            String authenticateHeaderValue = restAuthenticationService
                    .getWwwAuthenticateHeaderValue(request, response);

            response.setHeader("WWW-Authenticate", authenticateHeaderValue);
        }
        authenticationStatusRest.setProjection(projection);
        AuthenticationStatusResource authenticationStatusResource = converter.toResource(authenticationStatusRest);

        return authenticationStatusResource;
    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST})
    public ResponseEntity login(HttpServletRequest request, @RequestParam(name = "user", required = false) String user,
                                @RequestParam(name = "password", required = false) String password) {
        //If you can get here, you should be authenticated, the actual login is handled by spring security
        //see org.dspace.app.rest.security.StatelessLoginFilter

        //If we don't have an EPerson here, this means authentication failed and we should return an error message.
        return getLoginResponse(request,
                                "Authentication failed for user " + user + ": The credentials you provided are not " +
                                    "valid.");
    }

    @RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.PATCH,
            RequestMethod.DELETE })
    public ResponseEntity login() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Only POST is allowed for login requests.");
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity logout() {
        //This is handled by org.dspace.app.rest.security.CustomLogoutHandler
        return ResponseEntity.noContent().build();
    }

    protected ResponseEntity getLoginResponse(HttpServletRequest request, String failedMessage) {
        //Get the context and check if we have an authenticated eperson
        org.dspace.core.Context context = null;

        context = ContextUtil.obtainContext(request);

        if (context == null || context.getCurrentUser() == null) {
            // Note that the actual HTTP status in this case is set by
            // org.dspace.app.rest.security.StatelessLoginFilter.unsuccessfulAuthentication()
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(failedMessage);
        } else {
            //We have a user, so the login was successful.
            return ResponseEntity.ok().build();
        }
    }

}
