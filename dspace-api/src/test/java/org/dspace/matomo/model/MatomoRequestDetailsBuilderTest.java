/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import org.dspace.matomo.factory.MatomoRequestCountryEnricher;
import org.dspace.matomo.factory.MatomoRequestDetailsEnricher;
import org.dspace.matomo.factory.MatomoRequestDetailsEnricherFactory;
import org.dspace.matomo.factory.MatomoRequestIpAddressEnricher;
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

}