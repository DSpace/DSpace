/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import org.dspace.AbstractUnitTest;
import org.dspace.matomo.client.MatomoClient;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.dspace.matomo.model.MatomoRequestDetailsBuilder;
import org.dspace.usage.UsageEvent;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MatomoSyncEventHandlerTest extends AbstractUnitTest {

    @Mock
    MatomoClient matomoClient;
    @Mock
    MatomoRequestDetailsBuilder builder;
    @InjectMocks
    MatomoSyncEventHandler matomoSyncEventHandler;

    @Test
    public void testNullEvent() {
        matomoSyncEventHandler.handleEvent(null);
        Mockito.verifyNoInteractions(builder);
        Mockito.verifyNoInteractions(matomoClient);
    }

    @Test
    public void testSingleEvent() {
        UsageEvent usageEvent = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails = new MatomoRequestDetails();
        Mockito.when(builder.build(usageEvent)).thenReturn(matomoRequestDetails);

        matomoSyncEventHandler.handleEvent(usageEvent);

        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(matomoRequestDetails);
        Mockito.verifyNoMoreInteractions(matomoClient);
    }


    @Test
    public void testMultipleEvents() {
        UsageEvent usageEvent1 = Mockito.mock(UsageEvent.class);
        UsageEvent usageEvent2 = Mockito.mock(UsageEvent.class);
        UsageEvent usageEvent3 = Mockito.mock(UsageEvent.class);
        MatomoRequestDetails matomoRequestDetails1 = new MatomoRequestDetails();
        MatomoRequestDetails matomoRequestDetails2 = new MatomoRequestDetails();
        MatomoRequestDetails matomoRequestDetails3 = new MatomoRequestDetails();

        Mockito.when(builder.build(usageEvent1)).thenReturn(matomoRequestDetails1);
        Mockito.when(builder.build(usageEvent2)).thenReturn(matomoRequestDetails2);
        Mockito.when(builder.build(usageEvent3)).thenReturn(matomoRequestDetails3);

        matomoSyncEventHandler.handleEvent(usageEvent1);
        matomoSyncEventHandler.handleEvent(usageEvent2);
        matomoSyncEventHandler.handleEvent(usageEvent3);
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(matomoRequestDetails1);
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(matomoRequestDetails2);
        Mockito.verify(matomoClient, Mockito.times(1)).sendDetails(matomoRequestDetails3);
        Mockito.verifyNoMoreInteractions(matomoClient);
    }


}