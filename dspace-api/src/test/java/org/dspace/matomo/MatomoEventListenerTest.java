/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.usage.UsageEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MatomoEventListenerTest extends AbstractUnitTest {

    @Mock
    MatomoAsyncBulkRequestHandler matomoHandler1;
    @Mock
    MatomoSyncEventHandler matomoHandler2;
    @Mock
    ConfigurationService configurationService;
    @Mock
    BitstreamService bitstreamService;

    MatomoEventListener matomoEventListener;

    @Before
    public void setUp() throws Exception {
        matomoEventListener =
            new MatomoEventListener(List.of(matomoHandler1, matomoHandler2), configurationService, bitstreamService);
    }

    @Test
    public void testDisabledMatomo() {
        UsageEvent event = Mockito.mock(UsageEvent.class);

        matomoEventListener.receiveEvent(event);

        Mockito.verifyNoInteractions(matomoHandler1);
        Mockito.verifyNoInteractions(matomoHandler2);
    }


    @Test
    public void testDontHandleGenericViewEventWithMatomoEnabled() {
        UsageEvent event = Mockito.mock(UsageEvent.class);
        Mockito.when(event.getAction()).thenReturn(UsageEvent.Action.VIEW);
        Mockito.when(event.getObject()).thenReturn(Mockito.spy(Item.class));

        Mockito.when(configurationService.getBooleanProperty("matomo.enabled", false))
               .thenReturn(true);

        matomoEventListener.receiveEvent(event);

        Mockito.verifyNoInteractions(matomoHandler1);
        Mockito.verifyNoInteractions(matomoHandler2);
    }


    @Test
    public void testHandleBitstreamViewEvent() throws SQLException {
        // mock event
        UsageEvent event = Mockito.mock(UsageEvent.class);
        Mockito.when(event.getAction()).thenReturn(UsageEvent.Action.VIEW);

        // mock bitstream
        Bitstream bitstream = Mockito.spy(Bitstream.class);
        Mockito.when(
            bitstreamService.isInBundle(
                Mockito.eq(bitstream),
                Mockito.eq(Set.of(Constants.CONTENT_BUNDLE_NAME))
            )
        ).thenReturn(true);

        Mockito.when(event.getObject()).thenReturn(bitstream);

        // mock configuration
        Mockito.when(configurationService.getBooleanProperty(Mockito.eq("matomo.enabled"), Mockito.eq(false)))
               .thenReturn(true);
        Mockito.when(configurationService.getArrayProperty(Mockito.eq("matomo.track.bundles"), Mockito.any()))
               .thenReturn(new String[] { });

        matomoEventListener.receiveEvent(event);

        Mockito.verifyNoInteractions(matomoHandler1);
        Mockito.verifyNoInteractions(matomoHandler2);

        // none bundle, will skip processing
        Mockito.when(configurationService.getArrayProperty(Mockito.eq("matomo.track.bundles"), Mockito.any()))
               .thenReturn(new String[] {"none"});

        matomoEventListener.receiveEvent(event);

        Mockito.verifyNoMoreInteractions(matomoHandler1);
        Mockito.verifyNoMoreInteractions(matomoHandler2);

        // default ( original bundle only ) then proceed with the invocation
        Mockito.when(configurationService.getArrayProperty(Mockito.eq("matomo.track.bundles"), Mockito.any()))
               .thenReturn(new String[] { Constants.CONTENT_BUNDLE_NAME });

        matomoEventListener.receiveEvent(event);

        Mockito.verify(matomoHandler1, Mockito.times(1)).handleEvent(event);
        Mockito.verify(matomoHandler2, Mockito.times(1)).handleEvent(event);

    }

}