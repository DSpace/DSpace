/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for testing user authorization: `@PreAuthorize("hasAuthority('<AUTHORITY_NAME>')")`
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@RequestMapping(value = "/api/test/auth")
@RestController
public class ClarinAuthorizationTestController {

    // If the user has the authority `AUTHENTICATED`
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(value = "/authenticated", method = RequestMethod.GET)
    public ResponseEntity isAuthenticated(HttpServletRequest request) {
        return ResponseEntity.ok(true);
    }

    // If the user has the authority `UFAL`
    @PreAuthorize("hasAuthority('UFAL')")
    @RequestMapping(value = "/ufal", method = RequestMethod.GET)
    public ResponseEntity hasUfalAuthority(HttpServletRequest request) {
        return ResponseEntity.ok(true);
    }

    // If the user has the authority `UFAL_MEMBER`
    @PreAuthorize("hasAuthority('UFAL_MEMBER')")
    @RequestMapping(value = "/ufal-member", method = RequestMethod.GET)
    public ResponseEntity hasUfalMemberAuthority(HttpServletRequest request) {
        return ResponseEntity.ok(true);
    }
}
