/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.authentication;

import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author kevinvandevelde at atmire.com
 *
 * Spring redirects to the home page after a successfull logout. This success handles ensures that this is NOT the case.
 */
public class NoRedirectAuthenticationLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
    @PostConstruct
    public void afterPropertiesSet() {
        setRedirectStrategy(new NoRedirectStrategy());
    }

     protected class NoRedirectStrategy implements RedirectStrategy {

         @Override
         public void sendRedirect(HttpServletRequest request,
                                  HttpServletResponse response, String url) throws IOException {
             // no redirect

         }

     }
}
