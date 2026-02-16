/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saml2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.AbstractDSpaceTest;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;

public class DSpaceSamlAuthenticationSuccessHandlerTest extends AbstractDSpaceTest {

    private static ConfigurationService configurationService;

    private HttpServletRequest request;
    private RequestDispatcher requestDispatcher;
    private HttpServletResponse response;

    @BeforeClass
    public static void beforeAll() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Before
    public void beforeEach() {
        resetConfigurationService();

        request = Mockito.spy(new MockHttpServletRequest());
        requestDispatcher = Mockito.mock(RequestDispatcher.class);

        when(request.getRequestDispatcher("/api/authn/saml"))
            .thenReturn(requestDispatcher);

        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    public void testRequestIsForwardedToAuthEndpoint() throws Exception {
        Authentication auth = createAuthentication("rp-id", "name-id", Collections.emptyMap());
        DSpaceSamlAuthenticationSuccessHandler handler = new DSpaceSamlAuthenticationSuccessHandler();

        handler.onAuthenticationSuccess(request, response, auth);

        verify(requestDispatcher).forward(any(DSpaceSamlAuthRequest.class), any(HttpServletResponse.class));
    }

    @Test
    public void testStandardRequestAttributesAreSet() throws Exception {
        Map<String, List<Object>> samlAttributes = Map.ofEntries(
            Map.entry("saml-attr-name-1", List.of("attr-value-1")),
            Map.entry("saml-attr-name-2", List.of("attr-value-2"))
        );

        Authentication auth = createAuthentication("rp-id", "user-name-id", samlAttributes);
        DSpaceSamlAuthenticationSuccessHandler handler = new DSpaceSamlAuthenticationSuccessHandler();

        handler.onAuthenticationSuccess(request, response, auth);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(requestDispatcher).forward(requestCaptor.capture(), any(HttpServletResponse.class));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();

        assertEquals("rp-id", forwardedRequest.getAttribute("org.dspace.saml.RELYING_PARTY_ID"));
        assertEquals("user-name-id", forwardedRequest.getAttribute("org.dspace.saml.NAME_ID"));
        assertEquals(samlAttributes, forwardedRequest.getAttribute("org.dspace.saml.ATTRIBUTES"));
    }

    @Test
    public void testSamlAttributesAreMappedToRequestAttributes() throws Exception {
        Map<String, List<Object>> samlAttributes = Map.ofEntries(
            Map.entry("saml-given-name", List.of("Diana")),
            Map.entry("saml-family-name", List.of("Prince")),
            Map.entry("saml-email-address", List.of("wonder@justiceleague.org", "diana@prince.com")),
            Map.entry("not-mapped", List.of("unmapped value"))
        );

        configurationService.setProperty("saml-relying-party.rp-id.attributes",
            "saml-given-name => org.dspace.saml.GIVEN_NAME," +
            "saml-family-name => org.dspace.saml.SURNAME," +
            "saml-email-address => org.dspace.saml.EMAIL," +
            "saml-foo => org.dspace.saml.FOO"
        );

        Authentication auth = createAuthentication("rp-id", "user-name-id", samlAttributes);
        DSpaceSamlAuthenticationSuccessHandler handler = new DSpaceSamlAuthenticationSuccessHandler();

        handler.onAuthenticationSuccess(request, response, auth);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(requestDispatcher).forward(requestCaptor.capture(), any(HttpServletResponse.class));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();

        assertEquals(List.of("Diana"), forwardedRequest.getAttribute("org.dspace.saml.GIVEN_NAME"));
        assertEquals(List.of("Prince"), forwardedRequest.getAttribute("org.dspace.saml.SURNAME"));

        assertEquals(List.of("wonder@justiceleague.org", "diana@prince.com"),
            forwardedRequest.getAttribute("org.dspace.saml.EMAIL"));

        assertNull(forwardedRequest.getAttribute("org.dspace.saml.FOO"));
    }

    @Test
    public void testMisconfiguredAttributeMappingIsIgnored() throws Exception {
        Map<String, List<Object>> samlAttributes = Map.ofEntries(
            Map.entry("saml-given-name", List.of("Diana")),
            Map.entry("saml-family-name", List.of("Prince")),
            Map.entry("saml-email-address", List.of("wonder@justiceleague.org", "diana@prince.com")),
            Map.entry("not-mapped", List.of("unmapped value"))
        );

        configurationService.setProperty("saml-relying-party.rp-id.attributes",
            "saml-given-name => org.dspace.saml.GIVEN_NAME," +
            "saml-family-name > org.dspace.saml.SURNAME," + // oops
            "saml-email-address => org.dspace.saml.EMAIL"
        );

        Authentication auth = createAuthentication("rp-id", "user-name-id", samlAttributes);
        DSpaceSamlAuthenticationSuccessHandler handler = new DSpaceSamlAuthenticationSuccessHandler();

        handler.onAuthenticationSuccess(request, response, auth);

        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        verify(requestDispatcher).forward(requestCaptor.capture(), any(HttpServletResponse.class));

        HttpServletRequest forwardedRequest = requestCaptor.getValue();

        assertEquals(List.of("Diana"), forwardedRequest.getAttribute("org.dspace.saml.GIVEN_NAME"));
        assertNull(forwardedRequest.getAttribute("org.dspace.saml.SURNAME"));

        assertEquals(List.of("wonder@justiceleague.org", "diana@prince.com"),
            forwardedRequest.getAttribute("org.dspace.saml.EMAIL"));
    }

    private void resetConfigurationService() {
        ((DSpaceConfigurationService) configurationService).clear();

        configurationService.reloadConfig();
    }

    private Authentication createAuthentication(
        String relyingPartyRegistrationId, String name, Map<String, List<Object>> attributes
    ) {
        DefaultSaml2AuthenticatedPrincipal principal = new DefaultSaml2AuthenticatedPrincipal(name, attributes);

        principal.setRelyingPartyRegistrationId(relyingPartyRegistrationId);

        Authentication auth = new Saml2Authentication(principal, "<!-- doesn't matter -->", Collections.emptyList());

        return auth;
    }
}
