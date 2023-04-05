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

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Custom Spring authentication filter for Stateless authentication, intercepts requests to check for valid
 * authentication. This runs before *every* request in the DSpace backend to see if any authentication data
 * is passed in that request. If so, it authenticates the EPerson in the current Context.
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

        Authentication authentication;
        try {
            authentication = getAuthentication(req, res);
        } catch (AuthorizeException e) {
            // just return an error, but do not log
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
            return;
        } catch (IllegalArgumentException | SQLException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authentication request is invalid or incorrect");
            log.error("Authentication request is invalid or incorrect (status:{})",
                      HttpServletResponse.SC_BAD_REQUEST, e);
            return;
        } catch (AccessDeniedException e) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access is denied");
            log.error("Access is denied (status:{})", HttpServletResponse.SC_FORBIDDEN, e);
            return;
        }
        // If we have a valid Authentication, save it to Spring Security
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(req, res);
    }

    /**
     * This method returns an Authentication object
     * This Authentication object will be attempted to be for the eperson with the uuid in the parameter. Incase
     * this is able to be done properly, we'll be returning the EPerson Authentication.
     * If the Authentication object returned is not null, we'll be logged in as this EPerson given through from the
     * request.
     * If something goes wrong, we'll throw an IllegalArgumentException, AccessDeniedException or AuthorizeException
     * depending on what went wrong. This will be caught in the calling method and handled appropriately with the
     * corresponding response codes
     * @param request       The current request
     * @param res           The current response
     * @return              An Authentication object for the EPerson with the uuid in the parameter
     * @throws IOException  If something goes wrong
     */
    private Authentication getAuthentication(HttpServletRequest request, HttpServletResponse res)
        throws AuthorizeException, SQLException {

        if (restAuthenticationService.hasAuthenticationData(request)) {
            Context context = ContextUtil.obtainContext(request);
            // parse the token.
            EPerson eperson = restAuthenticationService.getAuthenticatedEPerson(request, res, context);
            if (eperson != null) {
                log.debug("Found authentication data in request for EPerson {}", eperson.getEmail());
                //Pass the eperson ID to the request service
                requestService.setCurrentUserId(eperson.getID());

                //Get the Spring authorities for this eperson
                List<GrantedAuthority> authorities = authenticationProvider.getGrantedAuthorities(context);
                String onBehalfOfParameterValue = request.getHeader(ON_BEHALF_OF_REQUEST_PARAM);
                if (onBehalfOfParameterValue != null) {
                    if (configurationService.getBooleanProperty("webui.user.assumelogin")) {
                        return getOnBehalfOfAuthentication(context, onBehalfOfParameterValue, res);
                    } else {
                        throw new IllegalArgumentException("The 'login as' feature is not allowed" +
                                                     " due to the current configuration");
                    }
                }

                //Return the Spring authentication object
                return new DSpaceAuthentication(eperson, authorities);
            } else {
                return null;
            }
        } else {
            if (request.getHeader(ON_BEHALF_OF_REQUEST_PARAM) != null) {
                throw new AuthorizeException("Must be logged in (as an admin) to use the 'login as' feature");
            }
        }

        return null;
    }

    private Authentication getOnBehalfOfAuthentication(Context context, String onBehalfOfParameterValue,
                                                       HttpServletResponse res) throws SQLException {

        if (!authorizeService.isAdmin(context)) {
            throw new AccessDeniedException("Only admins are allowed to use the login as feature");
        }
        UUID epersonUuid = UUIDUtils.fromString(onBehalfOfParameterValue);
        if (epersonUuid == null) {
            throw new IllegalArgumentException("The given UUID in the X-On-Behalf-Of header " +
                                                   "was not a proper UUID");
        }
        EPerson onBehalfOfEPerson = ePersonService.find(context, epersonUuid);
        if (onBehalfOfEPerson == null) {
            throw new IllegalArgumentException("The given UUID in the X-On-Behalf-Of header " +
                                                   "was not a proper EPerson UUID");
        }
        if (!authorizeService.isAdmin(context, onBehalfOfEPerson)) {
            requestService.setCurrentUserId(epersonUuid);
            context.switchContextUser(onBehalfOfEPerson);
            log.debug("Found 'on-behalf-of' authentication data in request for EPerson {}",
                      onBehalfOfEPerson.getEmail());
            return new DSpaceAuthentication(onBehalfOfEPerson,
                                            authenticationProvider.getGrantedAuthorities(context));
        } else {
            throw new IllegalArgumentException("You're unable to use the login as feature to log " +
                                                   "in as another admin");
        }
    }

}
