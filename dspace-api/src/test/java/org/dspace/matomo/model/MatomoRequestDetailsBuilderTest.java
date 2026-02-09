/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.matomo.factory.MatomoRequestCookieIdentifierEnricher;
import org.dspace.matomo.factory.MatomoRequestCookieSessionEnricher;
import org.dspace.matomo.factory.MatomoRequestCountryEnricher;
import org.dspace.matomo.factory.MatomoRequestCustomCookiesEnricher;
import org.dspace.matomo.factory.MatomoRequestCustomVariablesEnricher;
import org.dspace.matomo.factory.MatomoRequestDetailsEnricher;
import org.dspace.matomo.factory.MatomoRequestDetailsEnricherFactory;
import org.dspace.matomo.factory.MatomoRequestIpAddressEnricher;
import org.dspace.matomo.factory.MatomoRequestTrackerIdentifierParamEnricher;
import org.dspace.service.ClientInfoService;
import org.dspace.usage.UsageEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class MatomoRequestDetailsBuilderTest extends AbstractUnitTest {

    MatomoRequestDetailsBuilder builder;
    List<MatomoRequestDetailsEnricher> enrichers;

    @Mock
    UsageEvent usageEvent;
    @Mock
    HttpServletRequest request;
    @Mock
    Context context;

    final String siteId = "test";

    @Before
    public void setUp() throws Exception {
        enrichers = new ArrayList<>();
        builder = new MatomoRequestDetailsBuilder(enrichers, siteId);
    }

    @Test
    public void testDefaultBuilders() {
        MatomoRequestDetails request = builder.build(usageEvent);
        assertThat(request.parameters, CoreMatchers.notNullValue());
        assertThat(
            request.parameters,
            Matchers.hasEntry(
                Matchers.is("idsite"),
                Matchers.is(siteId)
            )
        );
        assertThat(
            request.parameters,
            Matchers.hasEntry(
                Matchers.is("rec"),
                Matchers.is("1")
            )
        );
    }

    @Test
    public void testActionNameBuilder() throws SQLException {
        enrichers.add(MatomoRequestDetailsEnricherFactory.actionNameEnricher());

        Item item = Mockito.mock(Item.class);
        Mockito.when(item.getName()).thenReturn("item-name");
        Mockito.when(item.getType()).thenReturn(Constants.ITEM);
        Mockito.when(this.usageEvent.getObject()).thenReturn(item);
        Mockito.when(this.usageEvent.getContext()).thenReturn(context);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("action_name"),
                Matchers.is("item-name")
            )
        );


        Bitstream bitstream = Mockito.mock(Bitstream.class);
        Mockito.when(bitstream.getType()).thenReturn(Constants.BITSTREAM);
        Mockito.when(this.usageEvent.getObject()).thenReturn(bitstream);

        try (MockedStatic<ContentServiceFactory> mock = Mockito.mockStatic(ContentServiceFactory.class)) {
            ContentServiceFactory serviceFactory = Mockito.mock(ContentServiceFactory.class);
            Mockito.when(ContentServiceFactory.getInstance()).thenReturn(serviceFactory);
            DSpaceObjectService<Bitstream> bitstreamService = Mockito.mock(BitstreamService.class);
            Mockito.when(serviceFactory.getDSpaceObjectService(bitstream))
                   .thenReturn(bitstreamService);
            Mockito.when(bitstreamService.getParentObject(context, bitstream))
                   .thenReturn(item);
            requestDetails = builder.build(usageEvent);
            assertThat(
                requestDetails.parameters,
                Matchers.hasEntry(
                    Matchers.is("action_name"),
                    Matchers.is("item-name")
                )
            );
        }

    }

    @Test
    public void testUserAgentEnricher() {
        enrichers.add(MatomoRequestDetailsEnricherFactory.userAgentEnricher());
        Mockito.when(request.getHeader(Mockito.eq("USER-AGENT"))).thenReturn("custom-agent");
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("ua"),
                Matchers.is("custom-agent")
            )
        );
    }

    @Test
    public void testUrlEnricher() {
        enrichers.add(MatomoRequestDetailsEnricherFactory.urlEnricher());

        Item item = Mockito.mock(Item.class);
        Mockito.when(item.getType()).thenReturn(Constants.ITEM);
        UUID itemUUID = UUID.randomUUID();
        Mockito.when(item.getID()).thenReturn(itemUUID);
        Mockito.when(this.usageEvent.getObject()).thenReturn(item);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("url"),
                Matchers.containsString("/items/" + itemUUID)
            )
        );

        UUID bitstreamUUID = UUID.randomUUID();
        Bitstream bitstream = Mockito.mock(Bitstream.class);
        Mockito.when(bitstream.getID()).thenReturn(bitstreamUUID);
        Mockito.when(this.usageEvent.getObject()).thenReturn(bitstream);

        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("url"),
                Matchers.containsString("/bitstreams/" + bitstreamUUID)
            )
        );

        UUID bundleUUID = UUID.randomUUID();
        DSpaceObject object = Mockito.mock(Bundle.class);
        Mockito.when(object.getType()).thenReturn(Constants.BUNDLE);
        Mockito.when(usageEvent.getObject()).thenReturn(object);

        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("url"),
                Matchers.emptyString()
            )
        );

        Mockito.when(usageEvent.getObject()).thenReturn(null);

        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("url"),
                Matchers.emptyString()
            )
        );

    }

    @Test
    public void testMatomoRequestCountryEnricher() {
        MatomoRequestCountryEnricher countryEnricher = new MatomoRequestCountryEnricher();
        enrichers.add(countryEnricher);

        String country = Locale.ITALIAN.getCountry();
        Mockito.when(request.getLocale()).thenReturn(Locale.ITALIAN);
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("country"),
                Matchers.is(country)
            )
        );

        Mockito.when(request.getLocale()).thenReturn(null);
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("country"),
                Matchers.emptyString()
            )
        );

        Mockito.when(usageEvent.getRequest()).thenReturn(null);
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("country"),
                Matchers.emptyString()
            )
        );
    }

    @Test
    public void testMatomoIpEnricher() {
        ClientInfoService clientInfo = Mockito.mock(ClientInfoService.class);
        MatomoRequestIpAddressEnricher ipAddressEnricher = new MatomoRequestIpAddressEnricher(clientInfo);
        enrichers.add(ipAddressEnricher);

        Mockito.when(usageEvent.getRequest()).thenReturn(request);
        Mockito.when(clientInfo.getClientIp(request)).thenReturn("fake-ip");

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("cip"),
                Matchers.is("fake-ip")
            )
        );

        Mockito.when(clientInfo.getClientIp(request)).thenReturn(null);
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("cip"),
                Matchers.emptyString()
            )
        );

        Mockito.when(clientInfo.getClientIp(request)).thenReturn("");
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("cip"),
                Matchers.emptyString()
            )
        );
    }

    @Test
    public void testDownloadEnricher() {
        enrichers.add(MatomoRequestDetailsEnricherFactory.downloadEnricher());

        Item item = Mockito.mock(Item.class);
        Mockito.when(item.getType()).thenReturn(Constants.ITEM);
        Mockito.when(this.usageEvent.getObject()).thenReturn(item);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("download"),
                Matchers.emptyString()
            )
        );

        UUID bitstreamUUID = UUID.randomUUID();
        Bitstream bitstream = Mockito.mock(Bitstream.class);
        Mockito.when(bitstream.getID()).thenReturn(bitstreamUUID);
        Mockito.when(this.usageEvent.getObject()).thenReturn(bitstream);

        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("download"),
                Matchers.containsString("/bitstreams/" + bitstreamUUID + "/download")
            )
        );

        Mockito.when(usageEvent.getObject()).thenReturn(null);

        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("download"),
                Matchers.emptyString()
            )
        );

    }

    @Test
    public void testMatomoCookieIdentifierEnricher() {
        MatomoRequestCookieIdentifierEnricher cookieEnricher = new MatomoRequestCookieIdentifierEnricher();
        enrichers.add(cookieEnricher);

        Cookie cookie = Mockito.mock(Cookie.class);
        Mockito.when(cookie.getName()).thenReturn("_pk_id.1.1fff");
        Mockito.when(cookie.getValue()).thenReturn("3225aebdb98b13f9.1740076196.");

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("_id"),
                Matchers.is("3225aebdb98b13f9")
            )
        );

        Mockito.when(request.getCookies()).thenReturn(null);
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.not(
                Matchers.hasEntry(
                    Matchers.is("_id"),
                    Matchers.any(String.class)
                )
            )
        );

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { });
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.not(
                Matchers.hasEntry(
                    Matchers.is("_id"),
                    Matchers.any(String.class)
                )
            )
        );

        Mockito.when(cookie.getName()).thenReturn("_pk_id.1.1fff");
        Mockito.when(cookie.getValue()).thenReturn("wrongvalue.1.2");
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.not(
                Matchers.hasEntry(
                    Matchers.is("_id"),
                    Matchers.any(String.class)
                )
            )
        );

        Mockito.when(cookie.getName()).thenReturn("_pk_id.1.1fff");
        Mockito.when(cookie.getValue()).thenReturn("");
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.not(
                Matchers.hasEntry(
                    Matchers.is("_id"),
                    Matchers.any(String.class)
                )
            )
        );
    }

    @Test
    public void testMatomoCustomCookieEnricher() {
        MatomoRequestCustomCookiesEnricher cookiesEnricher =
            new MatomoRequestCustomCookiesEnricher("_pk_ref,_pk_hsr,_pk_ses");
        enrichers.add(cookiesEnricher);

        Cookie pkRefCookie = Mockito.mock(Cookie.class);
        Mockito.when(pkRefCookie.getName()).thenReturn("_pk_ref.1.1fff");
        Mockito.when(pkRefCookie.getValue()).thenReturn("http://localhost/home");

        Cookie pkHsr = Mockito.mock(Cookie.class);
        Mockito.when(pkHsr.getName()).thenReturn("_pk_hsr.1.1fff");
        Mockito.when(pkHsr.getValue()).thenReturn("hsr-value");

        Cookie pkSes = Mockito.mock(Cookie.class);
        Mockito.when(pkSes.getName()).thenReturn("_pk_ses.1.1fff");
        Mockito.when(pkSes.getValue()).thenReturn("1");

        Cookie noCustom = Mockito.mock(Cookie.class);
        Mockito.when(noCustom.getName()).thenReturn("_pk_custom.1.1fff");

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { pkRefCookie, pkHsr, pkSes, noCustom });
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.cookies,
            Matchers.allOf(
                Matchers.hasEntry(
                    Matchers.is("_pk_ref.1.1fff"),
                    Matchers.is("http://localhost/home")
                ),
                Matchers.hasEntry(
                    Matchers.is("_pk_hsr.1.1fff"),
                    Matchers.is("hsr-value")
                ),
                Matchers.hasEntry(
                    Matchers.is("_pk_ses.1.1fff"),
                    Matchers.is("1")
                )
            )
        );

        assertThat(
            requestDetails.cookies,
            Matchers.not(
                Matchers.hasEntry(
                    Matchers.is("_pk_custom.1.1fff"),
                    Matchers.any(String.class)
                )
            )
        );
    }

    @Test
    public void testMatomoCookieSessionEnricher() {
        MatomoRequestCookieSessionEnricher sessionEnricher = new MatomoRequestCookieSessionEnricher();
        enrichers.add(sessionEnricher);

        Cookie sessionCookie = Mockito.mock(Cookie.class);
        Mockito.when(sessionCookie.getName()).thenReturn("MATOMO_SESSID");
        Mockito.when(sessionCookie.getValue()).thenReturn("44d4405e1652daa7a7e451c019cf01db");

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { sessionCookie });
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.cookies,
            Matchers.hasEntry(
                Matchers.is("MATOMO_SESSID"),
                Matchers.is("44d4405e1652daa7a7e451c019cf01db")
            )
        );
    }

    @Test
    public void testMatomoCustomVaribalesEnricher() {
        MatomoRequestCustomVariablesEnricher customVariablesEnricher = new MatomoRequestCustomVariablesEnricher();
        enrichers.add(customVariablesEnricher);

        Cookie cvar = Mockito.mock(Cookie.class);
        Mockito.when(cvar.getName()).thenReturn("_pk_cvar.1.1fff");
        Mockito.when(cvar.getValue()).thenReturn("{\"1\":[\"key1\",\"value1\"],\"2\":[\"key2\",\"value2\"]}");

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cvar });
        Mockito.when(usageEvent.getRequest()).thenReturn(request);

        MatomoRequestDetails requestDetails = builder.build(usageEvent);
        assertThat(
            requestDetails.parameters,
            Matchers.hasEntry(
                Matchers.is("_cvar"),
                Matchers.is("{\"1\":[\"key1\",\"value1\"],\"2\":[\"key2\",\"value2\"]}")
            )
        );
    }

    /**
     * Test the enrich method with an empty parameter map.
     * This tests the edge case where the request's parameter map is empty, which is implicitly handled in the method.
     */
    @Test
    public void testEnrichWithEmptyParameterMap() {
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        UsageEvent usageEvent = mock(UsageEvent.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(usageEvent.getRequest()).thenReturn(request);
        when(usageEvent.getRequest().getParameterMap()).thenReturn(new HashMap<>());

        MatomoRequestDetails result = enricher.enrich(usageEvent, matomoRequestDetails);

        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Test the enrich method with an invalid tracker ID.
     * This tests the edge case where the tracker ID is present but does not match the expected format.
     */
    @Test
    public void testEnrichWithInvalidTrackerId() {
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getRequest()).thenReturn(mock(HttpServletRequest.class));

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("trackerId", new String[] {"invalidTrackerID"});
        when(usageEvent.getRequest().getParameterMap()).thenReturn(parameterMap);

        MatomoRequestDetails result = enricher.enrich(usageEvent, matomoRequestDetails);

        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Test the enrich method with a UsageEvent that has a null request.
     * This tests the edge case where the UsageEvent's request is null, which is explicitly handled in the method.
     */
    @Test
    public void testEnrichWithNullRequest() {
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        UsageEvent usageEvent = mock(UsageEvent.class);
        when(usageEvent.getRequest()).thenReturn(null);

        MatomoRequestDetails result = enricher.enrich(usageEvent, matomoRequestDetails);

        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Test the enrich method with a null UsageEvent.
     * This tests the edge case where the UsageEvent is null, which is explicitly handled in the method.
     */
    @Test
    public void testEnrichWithNullUsageEvent() {
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();

        MatomoRequestDetails result = enricher.enrich(null, matomoRequestDetails);

        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Test case for the enrich method when the UsageEvent is null.
     * This test verifies that the method returns the original MatomoRequestDetails
     * object without modifications when the input UsageEvent is null.
     */
    @Test
    public void test_enrich_whenUsageEventIsNull() {
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        MatomoRequestDetails matomoRequestDetails = mock(MatomoRequestDetails.class);

        MatomoRequestDetails result = enricher.enrich(null, matomoRequestDetails);

        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Test case for enrich method when UsageEvent and HttpServletRequest are not null,
     * but the parameter map does not contain a valid tracker identifier.
     * Expected: The original MatomoRequestDetails should be returned unchanged.
     */
    @Test
    public void test_enrich_withInvalidParameter() {
        // Arrange
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        UsageEvent usageEvent = mock(UsageEvent.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("trackerId", new String[] {"invalidValue"});

        when(usageEvent.getRequest()).thenReturn(request);
        when(request.getParameterMap()).thenReturn(parameterMap);

        // Act
        MatomoRequestDetails result = enricher.enrich(usageEvent, matomoRequestDetails);

        // Assert
        assertEquals(matomoRequestDetails, result);
    }

    /**
     * Tests the enrich method when a valid tracker identifier is present in the request parameters.
     * This test verifies that the method adds the tracker identifier to the MatomoRequestDetails
     * when the usage event contains a valid tracker ID in its request parameters.
     */
    @Test
    public void test_enrich_with_valid_tracker_id() {
        // Arrange
        MatomoRequestTrackerIdentifierParamEnricher enricher = new MatomoRequestTrackerIdentifierParamEnricher();
        UsageEvent usageEvent = mock(UsageEvent.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();

        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("trackerId", new String[] {"1234567890abcdef"});

        when(usageEvent.getRequest()).thenReturn(request);
        when(request.getParameterMap()).thenReturn(parameterMap);

        MatomoRequestDetails result = enricher.enrich(usageEvent, matomoRequestDetails);

        assertThat(
            result.parameters,
            Matchers.hasEntry(
                Matchers.is("_id"),
                Matchers.is("1234567890abcdef")
            )
        );
    }

}