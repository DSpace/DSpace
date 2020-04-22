/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Custom Spring authentication filter for Stateless authentication, intercepts requests to check for valid
 * authentication
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class StatelessAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(StatelessAuthenticationFilter.class);

    private static final String ON_BEHALF_OF_REQUEST_PARAM = "X-On-Behalf-Of";

    private RestAuthenticationService restAuthenticationService;

    private EPersonRestAuthenticationProvider authenticationProvider;

    private RequestService requestService;

    private AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public StatelessAuthenticationFilter(AuthenticationManager authenticationManager,
                                         RestAuthenticationService restAuthenticationService,
                                         EPersonRestAuthenticationProvider authenticationProvider,
                                         RequestService requestService) {
        super(authenticationManager);
        this.requestService = requestService;
        this.restAuthenticationService = restAuthenticationService;
        this.authenticationProvider = authenticationProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        Pair<Authentication, Boolean> pair = getAuthentication(req, res);
        Authentication authentication = pair.getLeft();
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            restAuthenticationService.invalidateAuthenticationCookie(res);
        }
        if (pair.getRight()) {
            return;
        }
        chain.doFilter(req, res);
    }

    /**
     * This method will return a Pair instance with an Authentication object as the left side of the pair and a Boolean
     * for the right side of the pair which will indicate whether there was an error in the OnBehalfOf parsing or not
     * The Authentication object will be attempted to be for the eperson with the uuid in the parameter. Incase
     * this is able to be done properly, we'll be giving a pair back with the EPerson Authentication in the left side
     * and a false boolean as the right side.
     * If the Authentication object returned is not null, we'll be logged in as this EPerson given through from the
     * request. If the Boolean is true, we'll stop the execution and show a BadRequest error
     * @param request       The current request
     * @param res           The current response
     * @return              A Pair instance with the Authentication object on the left side and the boolean
     *                      indicating errors on the right side
     * @throws IOException  If something goes wrong
     */
    private Pair<Authentication, Boolean> getAuthentication(HttpServletRequest request, HttpServletResponse res)
        throws IOException {

        if (restAuthenticationService.hasAuthenticationData(request)) {
            // parse the token.

            Context context = ContextUtil.obtainContext(request);

            EPerson eperson = restAuthenticationService.getAuthenticatedEPerson(request, context);
            if (eperson != null) {
                //Pass the eperson ID to the request service
                requestService.setCurrentUserId(eperson.getID());

                //Get the Spring authorities for this eperson
                List<GrantedAuthority> authorities = authenticationProvider.getGrantedAuthorities(context, eperson);
                String onBehalfOfParameterValue = request.getHeader(ON_BEHALF_OF_REQUEST_PARAM);
                if (onBehalfOfParameterValue != null) {
                    if (configurationService.getBooleanProperty("webui.user.assumelogin")) {
                        return getOnBehalfOfAuthentication(context, onBehalfOfParameterValue, res);
                    } else {
                        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The login as feature is not allowed" +
                            " due to the current configuration");
                        return Pair.of(null, true);
                    }
                }

                //Return the Spring authentication object
                return Pair.of(new DSpaceAuthentication(eperson.getEmail(), authorities), false);
            } else {
                return Pair.of(null, false);
            }
        } else {
            if (request.getHeader(ON_BEHALF_OF_REQUEST_PARAM) != null) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only admins are allowed to use the" +
                    " login as feature");
                return Pair.of(null, true);
            }
        }

        return Pair.of(null, false);
    }

    private Pair<Authentication, Boolean> getOnBehalfOfAuthentication(Context context, String onBehalfOfParameterValue,
                                                       HttpServletResponse res) throws IOException {

        try {
            if (!authorizeService.isAdmin(context)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Only admins are allowed to use the" +
                    " login as feature");
                return Pair.of(null, true);
            }
            UUID epersonUuid = UUIDUtils.fromString(onBehalfOfParameterValue);
            if (epersonUuid == null) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given UUID in the X-On-Behalf-Of header " +
                    "was not a proper UUID");
                return Pair.of(null, true);
            }
            EPerson onBehalfOfEPerson = ePersonService.find(context, epersonUuid);
            if (onBehalfOfEPerson == null) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given UUID in the X-On-Behalf-Of header " +
                    "was not a proper EPerson UUID");
                return Pair.of(null, true);
            }
            if (!authorizeService.isAdmin(context, onBehalfOfEPerson)) {
                requestService.setCurrentUserId(epersonUuid);
                context.switchContextUser(onBehalfOfEPerson);
                return Pair.of(new DSpaceAuthentication(onBehalfOfEPerson.getEmail(),
                                authenticationProvider.getGrantedAuthorities(context, onBehalfOfEPerson)), false);
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "You're unable to use the login as feature to log " +
                    "in as another admin");
                return Pair.of(null, true);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return Pair.of(null, false);
        }
    }

}
