/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import org.dspace.app.rest.security.DSpaceCsrfTokenRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for the /api/security/csrf endpoint
 * <P>
 * NOTE: This test will autoconfigure the MockMvc (@AutoConfigureMockMvc) in order to avoid using
 * AbstractControllerIntegrationTest.getClient() because that method may use this /api/security/csrf endpoint to
 * obtain a CSRF token.
 **/
@AutoConfigureMockMvc
public class CsrfRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getCsrf() throws Exception {
        // NOTE: We avoid using getClient() here because that method may also call this "/api/security/csrf" endpoint.
        String headerToken = mockMvc.perform(get("/api/security/csrf"))
                                    .andExpect(status().isNoContent())
                                    // Expect this endpoint to send back the proper HTTP Header & Cookie
                                    // as set by DSpaceCsrfTokenRepository
                                    .andExpect(cookie().exists(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME))
                                    .andExpect(header().exists(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME))
                                    .andReturn().getResponse()
                                    .getHeader(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME);

        // Call the endpoint again, and verify we get a new token again. Endpoint should ALWAYS change the CSRF token
        String headerToken2 = mockMvc.perform(get("/api/security/csrf"))
                                    .andExpect(status().isNoContent())
                                    // Expect this endpoint to send back the proper HTTP Header & Cookie
                                    // as set by DSpaceCsrfTokenRepository
                                    .andExpect(cookie().exists(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME))
                                    .andExpect(header().exists(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME))
                                    .andReturn().getResponse()
                                    .getHeader(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME);

        assertNotEquals("CSRF Tokens should not be the same in separate requests", headerToken, headerToken2);
    }

    @Test
    public void getCsrfSendsCookieOnce() throws Exception {
        // Without a CSRF cookie in the request, a new token is generated
        MockHttpServletResponse response = mockMvc.perform(get("/api/security/csrf"))
                                                  .andExpect(status().isNoContent())
                                                  .andReturn().getResponse();
        String headerToken = response.getHeader(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME);
        Cookie[] csrfCookies = getCsrfCookies(response);
        assertEquals("CSRF cookie should be sent once", 1, csrfCookies.length);
        assertEquals("CSRF cookie should match the header", headerToken, csrfCookies[0].getValue());

        // With a CSRF cookie in the request, the existing token is sent back
        response = mockMvc.perform(get("/api/security/csrf").cookie(csrfCookies[0]))
                          .andExpect(status().isNoContent())
                          .andExpect(header().string(DSpaceCsrfTokenRepository.DSPACE_CSRF_HEADER_NAME,
                                                     headerToken))
                          .andReturn().getResponse();
        csrfCookies = getCsrfCookies(response);
        assertEquals("CSRF cookie should be sent once", 1, csrfCookies.length);
        assertEquals("CSRF cookie should match the header", headerToken, csrfCookies[0].getValue());
    }

    private Cookie[] getCsrfCookies(MockHttpServletResponse response) {
        return Arrays.stream(response.getCookies())
                     .filter(c -> DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME.equals(c.getName()))
                     .toArray(Cookie[]::new);
    }
}
