/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.core.StringContains;
import org.junit.Test;

public class SecurityRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void testCsrfEndpointWithoutCookieOrHeader() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/security/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        StringContains.containsString("DSPACE-XSRF-COOKIE")
                ));
    }

    @Test
    public void testCsrfEndpointWithoutHeader() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/security/csrf").cookie(
                new Cookie("DSPACE-XSRF-COOKIE", "5f12f98b-5de3-4ee1-bd5f-95b784bd17cf")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        StringContains.containsString("DSPACE-XSRF-COOKIE")
                ));
    }

    @Test
    public void testCsrfEndpointWithoutCookie() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/security/csrf").header("X-XSRF-TOKEN",
                        "5f12f98b-5de3-4ee1-bd5f-95b784bd17cf"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        StringContains.containsString("DSPACE-XSRF-COOKIE")
                ));
    }

    @Test
    public void testCsrfEndpointMismatchingCookieAndHeader() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/security/csrf").header("X-XSRF-TOKEN",
                        "5f12f98b-5de3-4ee1-bd5f-95b784bd17cf").cookie(
                        new Cookie("DSPACE-XSRF-COOKIE", "1427c36d-6d4c-423a-939d-8ddf0115b5c6")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        StringContains.containsString("DSPACE-XSRF-COOKIE")
                ));
    }

    @Test
    public void testCsrfEndpointMatchingCookieAndHeader() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/security/csrf").header("X-XSRF-TOKEN",
                        "5f12f98b-5de3-4ee1-bd5f-95b784bd17cf").cookie(
                        new Cookie("DSPACE-XSRF-COOKIE", "5f12f98b-5de3-4ee1-bd5f-95b784bd17cf")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

}
