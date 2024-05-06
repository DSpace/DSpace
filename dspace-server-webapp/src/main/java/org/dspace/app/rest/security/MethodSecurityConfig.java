/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * This EnableMethodSecurity configuration enables Spring Security annotation checks on all methods
 * (e.g. @PreAuthorize, @PostAuthorize annotations, etc.)
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    @Autowired
    private PermissionEvaluator dSpacePermissionEvaluator;

    /**
     * Tell Spring to use our custom PermissionEvaluator as part of method security.
     * This allows DSpacePermissionEvaluator to be used in @PreAuthorize annotations (and similar).
     * @see org.dspace.app.rest.security.DSpacePermissionEvaluator
     */
    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(dSpacePermissionEvaluator);
        return expressionHandler;
    }
}
