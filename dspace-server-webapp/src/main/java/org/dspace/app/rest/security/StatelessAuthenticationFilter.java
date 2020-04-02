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
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
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


    private boolean inErrorOnBehalfOf = false;

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

        inErrorOnBehalfOf = false;
        Authentication authentication = getAuthentication(req, res);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            restAuthenticationService.invalidateAuthenticationCookie(res);
        }
        if (inErrorOnBehalfOf) {
            return;
        }
        chain.doFilter(req, res);
    }

    private Authentication getAuthentication(HttpServletRequest request, HttpServletResponse res) throws IOException {

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
                    return getOnBehalfOfAuthentication(context, onBehalfOfParameterValue, request, res);
                }

                //Return the Spring authentication object
                return new DSpaceAuthentication(eperson.getEmail(), authorities);
            } else {
                return null;
            }
        } else {
            if (request.getHeader(ON_BEHALF_OF_REQUEST_PARAM) != null) {
                inErrorOnBehalfOf = true;
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "There is no logged in user");

            }
        }

        return null;
    }

    private Authentication getOnBehalfOfAuthentication(Context context, String onBehalfOfParameterValue,
                                                       HttpServletRequest request,
                                                       HttpServletResponse res) throws IOException {
        UUID epersonUuid = UUIDUtils.fromString(onBehalfOfParameterValue);
        if (epersonUuid == null) {
            inErrorOnBehalfOf = true;
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given UUID in the X-On-Behalf-Of header " +
                "was not a proper UUID");
            return null;
        }
        try {
            EPerson onBehalfOfEPerson = ePersonService.find(context, epersonUuid);
            if (onBehalfOfEPerson == null) {
                inErrorOnBehalfOf = true;
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given UUID in the X-On-Behalf-Of header " +
                    "was not a proper EPerson UUID");
                return null;
            }
            if (authorizeService.isAdmin(context)) {
                requestService.setCurrentUserId(epersonUuid);
                context.switchContextUser(onBehalfOfEPerson);
                return new DSpaceAuthentication(onBehalfOfEPerson.getEmail(),
                                                authenticationProvider.getGrantedAuthorities(context,
                                                                                             onBehalfOfEPerson));
            } else {
                inErrorOnBehalfOf = true;
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "The current user is not an admin");
                return null;
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
