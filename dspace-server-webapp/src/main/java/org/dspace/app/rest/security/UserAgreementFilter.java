/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static java.util.Arrays.asList;
import static org.dspace.app.rest.security.jwt.UserAgreementClaimProvider.USER_AGREEMENT_ACCEPTED;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.app.rest.security.jwt.UserAgreementClaimProvider;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that verify that the current logged user has accepted terms and
 * condition.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class UserAgreementFilter extends OncePerRequestFilter {

    private ConfigurationService configurationService;

    public UserAgreementFilter(ConfigurationService configurationService) {
        super();
        this.configurationService = configurationService;
    }

    /**
     * Set the response status as 401 Forbidden if the current user has not accepted
     * the user agreement. This check is performed only if the filter is enabled and
     * only if the current request path is not open. To verify if the user already
     * accepted the terms and conditions the request attribute set by the
     * {@link UserAgreementClaimProvider} is read.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Context context = ContextUtil.obtainContext(request);

        boolean filterEnabled = configurationService.getBooleanProperty("user-agreement.filter-enabled", false);
        if (!filterEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        EPerson currentUser = context.getCurrentUser();
        if (isNotOpenPath(request) && currentUser != null) {
            boolean isUserAgreementAccepted = BooleanUtils
                    .isTrue((Boolean) request.getAttribute(USER_AGREEMENT_ACCEPTED));
            if (!isUserAgreementAccepted) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("No user agreement accepted");
                return;
            }
        }

        filterChain.doFilter(request, response);

    }

    private boolean isNotOpenPath(HttpServletRequest request) {
        return asList(this.configurationService.getArrayProperty("user-agreement.open-path-patterns")).stream()
            .map(path -> new AntPathRequestMatcher(path))
            .noneMatch(openPath -> openPath.matches(request));
    }

}