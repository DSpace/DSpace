/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Define GET /api/security/csrf endpoint which may be used to obtain a CSRF token from Spring Security.
 * This is useful to force a CSRF token to be generated prior to a POST/PUT/PATCH request that requires it.
 * <P>
 * NOTE: This endpoint should be used sparingly to ensure clients are NOT performing two requests for every modifying
 * request (e.g. a GET /csrf followed by a POST/PUT/PATCH to another endpoint). Ideally, calling this endpoint is only
 * necessary BEFORE the first POST/PUT/PATCH (if a CSRF token has not yet been obtained), or in scenarios where the
 * client must *force* the CSRF token to be reloaded.
 */
@RequestMapping(value = "/api/security")
@RestController
public class CsrfRestController {

    @Lazy
    @Autowired
    CsrfTokenRepository csrfTokenRepository;

    /**
     * Return the current CSRF token as defined by Spring Security.
     * Inspired by
     * https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_i_am_using_a_single_page_application_with_httpsessioncsrftokenrepository
     * @param request HTTP Request
     * @param response HTTP response
     * @param csrfToken injected CsrfToken by Spring Security
     * @return An empty response with CSRF in header & cookie
     */
    @GetMapping("/csrf")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RepresentationModel<?>> getCsrf(HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          CsrfToken csrfToken) {
        // Save the CSRF token to our response using the currently enabled CsrfTokenRepository
        csrfTokenRepository.saveToken(csrfToken, request, response);

        // Return a 204 No Content status
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
