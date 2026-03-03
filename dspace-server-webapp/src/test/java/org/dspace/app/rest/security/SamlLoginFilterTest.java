/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.AbstractDSpaceTest;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;

public class SamlLoginFilterTest extends AbstractDSpaceTest {
    private static ConfigurationService configurationService;

    private AuthenticationManager authManager;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RestAuthenticationService restAuthService;
    private FilterChain filterChain;
    private SamlLoginFilter filter;

    @BeforeClass
    public static void beforeAll() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Before
    public void beforeEach() throws Exception {
        resetConfigurationService();

        authManager = createAuthenticationManager();
        restAuthService = createRestAuthenticationService();
        filterChain = Mockito.mock(FilterChain.class);
        filter = new SamlLoginFilter("/api/authn/saml", HttpMethod.GET.name(), authManager, restAuthService);
        request = createRequest("/api/authn/saml");
        response = createResponse();
    }

    @Test
    public void testRedirectAfterSuccess() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            "org.dspace.authenticate.SamlAuthentication");

        configurationService.setProperty("dspace.ui.url","http://dspace.example.org");
        configurationService.setProperty("dspace.server.url","http://dspace.example.org/server");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("http://dspace.example.org");
    }

    @Test
    public void testRedirectToRemoteHostNotAllowed() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            "org.dspace.authenticate.SamlAuthentication");

        configurationService.setProperty("dspace.ui.url","http://different.host.bad");
        configurationService.setProperty("dspace.server.url","http://dspace.example.org/server");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(eq(400), anyString());
    }

    @Test
    public void testRedirectToRemoteHostCorsAllowed() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            "org.dspace.authenticate.SamlAuthentication");

        configurationService.setProperty("rest.cors.allowed-origins", "http://different.host.ok");
        configurationService.setProperty("dspace.ui.url","http://different.host.ok");
        configurationService.setProperty("dspace.server.url","http://dspace.example.org/server");

        filter.doFilter(request, response, filterChain);

        verify(response).sendRedirect("http://different.host.ok");
    }

    @Test
    public void testAuthCookieSaved() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            "org.dspace.authenticate.SamlAuthentication");

        configurationService.setProperty("dspace.ui.url","http://dspace.example.org");
        configurationService.setProperty("dspace.server.url","http://dspace.example.org/server");

        filter.doFilter(request, response, filterChain);

        verify(restAuthService).addAuthenticationDataForUser(
            eq(request), eq(response), any(DSpaceAuthentication.class), eq(true));
    }

    @Test
    public void testSamlAuthenticationNotEnabled() throws Exception {
        assertThrows(ProviderNotFoundException.class, () -> filter.attemptAuthentication(request, response));
    }

    private void resetConfigurationService() {
        ((DSpaceConfigurationService) configurationService).clear();

        configurationService.reloadConfig();
    }

    private AuthenticationManager createAuthenticationManager() {
        AuthenticationManager mockAuthManager = Mockito.mock(AuthenticationManager.class);

        when(mockAuthManager.authenticate(any(Authentication.class)))
            .thenReturn(Mockito.mock(DSpaceAuthentication.class));

        return mockAuthManager;
    }

    private RestAuthenticationService createRestAuthenticationService() throws Exception {
        RestAuthenticationService mockRestAuthService = Mockito.mock(RestAuthenticationService.class);

        doNothing().when(mockRestAuthService).addAuthenticationDataForUser(
            isA(HttpServletRequest.class), isA(HttpServletResponse.class),
            isA(DSpaceAuthentication.class), isA(Boolean.class));

        return mockRestAuthService;
    }

    private HttpServletRequest createRequest(String path) {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(HttpMethod.GET.name(), path);

        mockRequest.setPathInfo(path);

        return mockRequest;
    }

    private HttpServletResponse createResponse() throws Exception {
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

        doNothing().when(mockResponse).sendRedirect(isA(String.class));
        doNothing().when(mockResponse).sendError(isA(Integer.class), isA(String.class));

        return mockResponse;
    }
}
