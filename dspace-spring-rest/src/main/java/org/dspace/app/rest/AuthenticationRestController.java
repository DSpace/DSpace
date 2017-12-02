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

import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.AuthenticationStatusRest;
import org.dspace.app.rest.model.hateoas.AuthenticationStatusResource;
import org.dspace.app.rest.model.hateoas.AuthnResource;
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
 * @author Atmire NV (info at atmire dot com)
 */
@RequestMapping(value = "/api/" + AuthnRest.CATEGORY)
@RestController
public class AuthenticationRestController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationRestController.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private Utils utils;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService.register(this, Arrays.asList(new Link("/api/" + AuthnRest.CATEGORY, AuthnRest.NAME)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public AuthnResource authn() throws SQLException {
        return new AuthnResource(new AuthnRest(), utils);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public AuthenticationStatusResource status(HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPersonRest ePersonRest = null;
        if (context.getCurrentUser() != null) {
            ePersonRest = ePersonConverter.fromModel(context.getCurrentUser());
        }
        AuthenticationStatusResource authenticationStatusResource = new AuthenticationStatusResource( new AuthenticationStatusRest(ePersonRest), utils);
        return authenticationStatusResource;
    }

    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity login(HttpServletRequest request, @RequestParam(name = "user", required = false) String user,
                                @RequestParam(name = "password", required = false) String password) {
        //If you can get here, you should be authenticated, the actual login is handled by spring security
        //see org.dspace.app.rest.security.StatelessLoginFilter

        //If we don't have an EPerson here, this means authentication failed and we should return an error message.

        return getLoginResponse(request, "Authentication failed for user " + user + ": The credentials you provided are not valid.");
    }

    //TODO This should be moved under API, but then we also need to update org.dspace.authenticate.ShibAuthentication
    //This is also not gonna work until it is moved
    @RequestMapping(value = "/shibboleth-login", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity shibbolethLogin(HttpServletRequest request) {
        //If you can get here, you should be authenticated, the actual login is handled by spring security.
        //If not, no valid Shibboleth session is present or Shibboleth config is missing.

        /* Make sure to apply
           - AuthType shibboleth
           - ShibRequireSession On
           - ShibUseHeaders On
           - require valid-user
           to this endpoint. The Shibboleth daemon will then take care of redirecting you to the login page if
           necessary.
         */

        //TODO we should redirect the user to a correct page in the UI. These could be provided as optional parameters.
        return getLoginResponse(request, "Shibboleth authentication failed: No valid Shibboleth session could be found.");
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity logout() {
        //This is handled by org.dspace.app.rest.security.CustomLogoutHandler
        return ResponseEntity.ok().build();
    }

    protected ResponseEntity getLoginResponse(HttpServletRequest request, String failedMessage) {
        //Get the context and check if we have an authenticated eperson
        org.dspace.core.Context context = null;

        context = ContextUtil.obtainContext(request);


        if(context == null || context.getCurrentUser() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(failedMessage);
        } else {
            //We have a user, so the login was successful.
            return ResponseEntity.ok().build();
        }
    }


}
