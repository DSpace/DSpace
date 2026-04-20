/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * DSpace-specific concepts and behaviors to support Spring Security.
 * These may be used by Spring EL expressions in Spring Security annotations.
 *
 * <p>{@code hasPermission} terms are evaluated by
 * {@link DSpacePermissionEvaluator}, an implementation of Spring's
 * {@link PermissionEvaluator}.  It tests access to specific model objects
 * (Item, EPerson etc.) using those objects' policies.  It is injected with a
 * collection of {@link RestPermissionEvaluatorPlugin}s which do the work.
 *
 * <p>{@code hasAuthority} terms are implemented by {@link GrantedAuthority}
 * implementations such as {@link EPersonRestAuthenticationProvider}.  These
 * test for authorization properties of the session itself, such as membership
 * in the site administrators group.
 *
 * <p>{@code *PermissionEvaluatorPlugin} classes test permission for specific
 * types of objects.  They implement {@link RestPermissionEvaluatorPlugin}.
 *
 * <p>Other classes TBD:
 * <ul>
 *   <li>*Filter</li>
 *   <li>*Configuration</li>
 * </ul>
 */
package org.dspace.app.rest.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.GrantedAuthority;
