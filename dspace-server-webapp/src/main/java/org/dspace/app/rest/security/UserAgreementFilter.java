/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that verify that the current logged user has accepted terms and
 * condition.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class UserAgreementFilter extends OncePerRequestFilter {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        Context context = ContextUtil.obtainContext(request);

        boolean filterEnabled = configurationService.getBooleanProperty("user-agreement.filter-enabled", false);

        if (isAdmin(context) || !filterEnabled) {
            filterChain.doFilter(request, response);
        }

        EPerson currentUser = context.getCurrentUser();
        if (isNotAllowedPath(request) && currentUser != null) {
            String value = getUserAgreementMetadataValue(context, currentUser);
            if (value == null || !value.equals("true")) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("No user agreement accepted");
                return;
            }
        }

        filterChain.doFilter(request, response);

    }

    private boolean isAdmin(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNotAllowedPath(HttpServletRequest request) {
        return asList(this.configurationService.getArrayProperty("user-agreement.allowed-path-patterns")).stream()
            .map(path -> new AntPathRequestMatcher(path))
            .noneMatch(allowedPath -> allowedPath.matches(request));
    }

    private String getUserAgreementMetadataValue(Context context, EPerson eperson) {
        return ePersonService.getMetadata(eperson, "dspace", "agreements", "end-user", Item.ANY).stream()
            .map(MetadataValue::getValue)
            .findFirst()
            .orElse(null);
    }

}