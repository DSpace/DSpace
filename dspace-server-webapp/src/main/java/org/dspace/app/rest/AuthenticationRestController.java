/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.AuthenticationStatusRest;
import org.dspace.app.rest.model.AuthenticationTokenRest;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.AuthenticationStatusResource;
import org.dspace.app.rest.model.hateoas.AuthenticationTokenResource;
import org.dspace.app.rest.model.hateoas.AuthnResource;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.wrapper.AuthenticationToken;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.service.ClientInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private ClientInfoService clientInfoService;

    @Autowired
    private Utils utils;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(Link.of("/api/" + AuthnRest.CATEGORY, AuthnRest.NAME)));
    }

    @RequestMapping(method = RequestMethod.GET)
    public AuthnResource authn() {
        AuthnRest authnRest = new AuthnRest();
        authnRest.setProjection(utils.obtainProjection());
        return converter.toResource(authnRest);
    }

    /**
     * Check the current user's authentication status (i.e. whether they are authenticated or not)
     * <P>
     * If the user is NOT currently authenticated, a list of all currently enabled DSpace authentication endpoints
     * is returned in the WWW-Authenticate header.
     * @param request current request
     * @param response response
     * @return AuthenticationStatusResource
     * @throws SQLException
     */
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public AuthenticationStatusResource status(HttpServletRequest request, HttpServletResponse response)
            throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPersonRest ePersonRest = null;
        Projection projection = utils.obtainProjection();
        if (context.getCurrentUser() != null) {
            ePersonRest = converter.toRest(context.getCurrentUser(), projection);
        }
        List<GroupRest> groupList = context.getSpecialGroups().stream()
                .map(g -> (GroupRest) converter.toRest(g, projection)).collect(Collectors.toList());

        AuthenticationStatusRest authenticationStatusRest = new AuthenticationStatusRest(ePersonRest);
        // When not authenticated add WWW-Authenticate so client can retrieve all available authentication methods
        if (!authenticationStatusRest.isAuthenticated()) {
            String authenticateHeaderValue = restAuthenticationService
                    .getWwwAuthenticateHeaderValue(request, response);

            response.setHeader("WWW-Authenticate", authenticateHeaderValue);
        }
        authenticationStatusRest.setAuthenticationMethod(context.getAuthenticationMethod());
        authenticationStatusRest.setProjection(projection);
        authenticationStatusRest.setSpecialGroups(groupList);

        AuthenticationStatusResource authenticationStatusResource = converter.toResource(authenticationStatusRest);

        return authenticationStatusResource;
    }

    /**
     * Check the current user's authentication status (i.e. whether they are authenticated or not) and,
     * if authenticated, retrieves the current context's special groups.
     * @param page
     * @param assembler
     * @param request
     * @param response
     * @return
     * @throws SQLException
     */
    @RequestMapping(value = "/status/specialGroups", method = RequestMethod.GET)
    public EntityModel retrieveSpecialGroups(Pageable page, PagedResourcesAssembler assembler,
                HttpServletRequest request, HttpServletResponse response)
            throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Projection projection = utils.obtainProjection();

        List<GroupRest> groupList = context.getSpecialGroups().stream()
                .map(g -> (GroupRest) converter.toRest(g, projection)).collect(Collectors.toList());
        Page<GroupRest> groupPage = (Page<GroupRest>) utils.getPage(groupList, page);
        Link link = linkTo(
                methodOn(AuthenticationRestController.class).retrieveSpecialGroups(page, assembler, request, response))
                        .withSelfRel();

        return EntityModel.of(new EmbeddedPage(link.getHref(),
                groupPage.map(converter::toResource), null, "specialGroups"));
    }

    /**
     * Check whether the login has succeeded or not. The actual login is performed by one of the enabled login filters
     * (e.g. {@link org.dspace.app.rest.security.StatelessLoginFilter}).
     * See {@link org.dspace.app.rest.security.WebSecurityConfiguration} for enabled login filters.
     *
     * @param request current request
     * @param user user
     * @param password password
     * @return ResponseEntity with information about whether login was successful or failed
     */
    @RequestMapping(value = "/login", method = {RequestMethod.POST})
    public ResponseEntity login(HttpServletRequest request, @RequestParam(name = "user", required = false) String user,
                                @RequestParam(name = "password", required = false) String password) {
        //If you can get here, you should be authenticated, the actual login is handled by spring security

        // Build our response. This will check if we have an EPerson.
        // If not, that means the authentication failed and we should return the error message
        return getLoginResponse(request,
                                "Authentication failed. The credentials you provided are not valid.");
    }

    /**
     * This method will generate a short lived token to be used for bitstream downloads among other things.
     *
     * curl -v -X POST https://{dspace-server.url}/api/authn/shortlivedtokens -H "Authorization: Bearer eyJhbG...COdbo"
     *
     * Example:
     * <pre>
     * {@code
     * curl -v -X POST https://{dspace-server.url}/api/authn/shortlivedtokens -H "Authorization: Bearer eyJhbG...COdbo"
     * }
     * </pre>
     * @param request The StandardMultipartHttpServletRequest
     * @return        The created short lived token
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(value = "/shortlivedtokens", method = RequestMethod.POST)
    public AuthenticationTokenResource shortLivedToken(HttpServletRequest request) {
        return shortLivedTokenResponse(request);
    }

    /**
     * This method will generate a short lived token to be used for bitstream downloads among other things.
     *
     * For security reasons, this endpoint only responds to a explicitly defined list of ips.
     *
     * curl -v -X GET https://{dspace-server.url}/api/authn/shortlivedtokens -H "Authorization: Bearer eyJhbG...COdbo"
     *
     * Example:
     * <pre>
     * {@code
     * curl -v -X GET https://{dspace-server.url}/api/authn/shortlivedtokens -H "Authorization: Bearer eyJhbG...COdbo"
     * }
     * </pre>
     * @param request The StandardMultipartHttpServletRequest
     * @return        The created short lived token
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(value = "/shortlivedtokens", method = RequestMethod.GET)
    public AuthenticationTokenResource shortLivedTokenViaGet(HttpServletRequest request) throws AuthorizeException {
        if (!clientInfoService.isRequestFromTrustedProxy(request.getRemoteAddr())) {
            throw new AuthorizeException("Requests to this endpoint should be made from a trusted IP address.");
        }

        return shortLivedTokenResponse(request);
    }

    /**
     * See {@link #shortLivedToken} and {@link #shortLivedTokenViaGet}
     */
    private AuthenticationTokenResource shortLivedTokenResponse(HttpServletRequest request) {
        Projection projection = utils.obtainProjection();
        AuthenticationToken shortLivedToken =
                restAuthenticationService.getShortLivedAuthenticationToken(ContextUtil.obtainContext(request), request);
        AuthenticationTokenRest authenticationTokenRest = converter.toRest(shortLivedToken, projection);
        return converter.toResource(authenticationTokenRest);
    }

    /**
     * Disables GET/PUT/PATCH on the /login endpoint. You must use POST (see above method)
     * @return ResponseEntity
     */
    @RequestMapping(value = "/login", method = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.PATCH,
            RequestMethod.DELETE })
    public ResponseEntity login() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Only POST is allowed for login requests.");
    }

    /**
     * Returns a successful "204 No Content" response for a logout request.
     * Actual logout is performed by our {@link org.dspace.app.rest.security.CustomLogoutHandler}
     * <P>
     * For logout we *require* POST requests. HEAD is also supported for endpoint visibility in HAL Browser, etc.
     * @return ResponseEntity (204 No Content)
     */
    @RequestMapping(value = "/logout", method = {RequestMethod.HEAD, RequestMethod.POST})
    public ResponseEntity logout() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Disables GET/PUT/PATCH on the /logout endpoint. You must use POST (see above method)
     * @return ResponseEntity
     */
    @RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.PATCH,
        RequestMethod.DELETE })
    public ResponseEntity logoutMethodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Only POST is allowed for logout requests.");
    }

    /**
     * Check the request to see if the login succeeded or failed.
     * If the request includes a valid EPerson, then it was successful.
     * If the request does not include a valid EPerson, then return the failedMessage.
     * <P>
     * NOTE: This method assumes that a login filter (e.g. {@link org.dspace.app.rest.security.StatelessLoginFilter})
     * has already attempted the authentication and, if successful, added EPerson data to the current request.
     * @param request current request
     * @param failedMessage message to send if no EPerson found
     * @return ResponseEntity
     */
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
