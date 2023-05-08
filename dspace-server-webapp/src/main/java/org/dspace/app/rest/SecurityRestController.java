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

import org.dspace.app.rest.security.DSpaceCsrfTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/api/security")
@RestController
public class SecurityRestController {
    DSpaceCsrfTokenRepository csrfTokenRepository = new DSpaceCsrfTokenRepository();

    @RequestMapping(value = "/csrf", method = RequestMethod.POST)
    public ResponseEntity csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        CsrfToken headerToken = csrfTokenRepository.loadTokenFromHeader(request);
        if (token == null || headerToken == null || !token.getToken().equals(headerToken.getToken())) {
            CsrfToken newToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(newToken, request, response);
        }
        return ResponseEntity.noContent().build();
    }

}
