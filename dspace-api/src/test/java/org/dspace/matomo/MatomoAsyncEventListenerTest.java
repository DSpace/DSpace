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
import org.dspace.services.ConfigurationService;
import org.dspace.usage.UsageEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MatomoAsyncEventListenerTest extends AbstractUnitTest {

    @Mock
    MatomoAsyncDequeHandler matomoHandler1;
    @Mock
    MatomoSyncEventHandler matomoHandler2;
    @Mock
    ConfigurationService configurationService;

    MatomoAsyncEventListener matomoAsyncEventListener;

    @Before
    public void setUp() throws Exception {
        matomoAsyncEventListener =
            new MatomoAsyncEventListener(List.of(matomoHandler1, matomoHandler2), configurationService);
    }

    @Test
    public void testDisabledMatomo() {
        UsageEvent event = Mockito.mock(UsageEvent.class);

        matomoAsyncEventListener.receiveEvent(event);

        Mockito.verifyNoInteractions(matomoHandler1);
        Mockito.verifyNoInteractions(matomoHandler2);
    }


    @Test
    public void testHandleEvent() {
        UsageEvent event = Mockito.mock(UsageEvent.class);

        Mockito.when(configurationService.getBooleanProperty("matomo.enabled", false))
               .thenReturn(true);

        matomoAsyncEventListener.receiveEvent(event);

        Mockito.verify(matomoHandler1, Mockito.times(1)).handleEvent(event);
        Mockito.verify(matomoHandler2, Mockito.times(1)).handleEvent(event);
        Mockito.verifyNoMoreInteractions(matomoHandler1, matomoHandler2);
    }

}