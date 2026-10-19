/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.matomo.client.MatomoClient;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.matomo.model.MatomoRequestDetailsBuilder;
import org.dspace.usage.UsageEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MatomoAsyncBulkRequestHandlerTest extends AbstractUnitTest {

    @Mock
    MatomoRequestDetailsBuilder builder;
    @Mock
    MatomoClient matomoClient;

    MatomoAsyncBulkRequestHandler matomoAsyncDequeHandler;

    @Before
    public void setUp() throws Exception {
        matomoAsyncDequeHandler =
            new MatomoAsyncBulkRequestHandler(builder, matomoClient, 5);
    }

    @Test
    public void testNullEvent() {
        matomoAsyncDequeHandler.handleEvent(null);
        Mockito.verifyNoInteractions(matomoClient);
        Mockito.verifyNoInteractions(builder);
    }

    @Test
    public void testSingleRequestHigherCapacity() {
        UsageEvent usageEvent = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        Mockito.when(builder.build(usageEvent)).thenReturn(matomoRequestDetails);

        matomoAsyncDequeHandler.handleEvent(usageEvent);

        Mockito.verifyNoInteractions(matomoClient);
    }


    @Test
    public void testSingleRequestCapacity() {
        matomoAsyncDequeHandler =
            new MatomoAsyncBulkRequestHandler(builder, matomoClient, 1);

        UsageEvent usageEvent = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        Mockito.when(builder.build(usageEvent)).thenReturn(matomoRequestDetails);

        matomoAsyncDequeHandler.handleEvent(usageEvent);

        Mockito.verify(matomoClient, Mockito.times(1))
               .sendDetails(Mockito.any(List.class));
    }


    @Test
    public void testTwoRequestsTwoCapacity() {
        matomoAsyncDequeHandler =
            new MatomoAsyncBulkRequestHandler(builder, matomoClient, 3);

        UsageEvent usageEvent = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        Mockito.when(builder.build(usageEvent)).thenReturn(matomoRequestDetails);

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.never()).sendDetails(Mockito.any(List.class));

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(Mockito.any(List.class));

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(Mockito.any(List.class));

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.times(2)).sendDetails(Mockito.any(List.class));
    }

    @Test
    public void testManualSendEvents() {
        UsageEvent usageEvent = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        Mockito.when(builder.build(usageEvent)).thenReturn(matomoRequestDetails);

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.never()).sendDetails(Mockito.any(List.class));

        matomoAsyncDequeHandler.handleEvent(usageEvent);
        Mockito.verify(matomoClient, Mockito.never()).sendDetails(Mockito.any(List.class));

        matomoAsyncDequeHandler.sendEvents();
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(Mockito.any(List.class));
    }


}