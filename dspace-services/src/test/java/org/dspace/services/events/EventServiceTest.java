/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.events;

import static org.junit.Assert.*;

import org.dspace.services.RequestService;
import org.dspace.services.model.Event;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Event service testing (not wrapped in requests)
 * 
 * @author Aaron Zeckoski (azeckoski@gmail.com) - azeckoski - 12:24:17 PM Nov 20, 2008
 */
public class EventServiceTest extends DSpaceAbstractKernelTest {

    private SystemEventService eventService;
    private RequestService requestService;
    private EventListenerNoFilter listenerNoFilter;
    private EventListenerNameFilter listenerNameFilter;
    private EventListenerBothFilters listenerBothFilters;

    @Before
    public void init() {
        eventService = getService(SystemEventService.class);
        requestService = getService(RequestService.class);
        // create the filters
        listenerNoFilter = new EventListenerNoFilter();
        listenerNameFilter = new EventListenerNameFilter();
        listenerBothFilters = new EventListenerBothFilters();
        // register the filters
        eventService.registerEventListener(listenerNoFilter);
        eventService.registerEventListener(listenerNameFilter);
        eventService.registerEventListener(listenerBothFilters);
    }

    @After
    public void tearDown() {
        // need to do this to ensure the resources are freed up by junit
        eventService = null;
        requestService = null;
        listenerNoFilter = null;
        listenerNameFilter = null;
        listenerBothFilters = null;
    }

    /**
     * Test method for {@link org.dspace.services.events.SystemEventService#fireEvent(org.dspace.services.model.Event)}.
     */
    @Test
    public void testFireEvent() {
        Event event1 = new Event("test.event.read", "test-resource-1", "11111", false);
        Event event2 = new Event("test.event.jump", null, "11111", false);
        Event event3 = new Event("some.event.write", "test-resource-2", "11111", true);
        Event event4 = new Event("some.event.add", "fake-resource-555", "11111", true);
        Event event5 = new Event("test.event.read", "test-resource-2", "11111", false);
        Event event6 = new Event("test.event.read", "fake-resource-555", "11111", false);
        Event event7 = new Event("aaron.event.read", "az-123456", "11111", false);

        eventService.fireEvent( event1 );
        eventService.fireEvent( event2 );
        eventService.fireEvent( event3 );
        eventService.fireEvent( event4 );
        eventService.fireEvent( event5 );
        eventService.fireEvent( event6 );
        eventService.fireEvent( event7 );

        // now check that the listeners were properly triggered
        assertEquals(7, listenerNoFilter.getReceivedEvents().size());
        assertEquals(5, listenerNameFilter.getReceivedEvents().size());
        assertEquals(3, listenerBothFilters.getReceivedEvents().size());

        // now verify the set of events and order
        assertEquals(event1, listenerNoFilter.getReceivedEvents().get(0));
        assertEquals(event2, listenerNoFilter.getReceivedEvents().get(1));
        assertEquals(event3, listenerNoFilter.getReceivedEvents().get(2));
        assertEquals(event4, listenerNoFilter.getReceivedEvents().get(3));
        assertEquals(event5, listenerNoFilter.getReceivedEvents().get(4));
        assertEquals(event6, listenerNoFilter.getReceivedEvents().get(5));
        assertEquals(event7, listenerNoFilter.getReceivedEvents().get(6));

        assertEquals(event1, listenerNameFilter.getReceivedEvents().get(0));
        assertEquals(event2, listenerNameFilter.getReceivedEvents().get(1));
        assertEquals(event5, listenerNameFilter.getReceivedEvents().get(2));
        assertEquals(event6, listenerNameFilter.getReceivedEvents().get(3));
        assertEquals(event7, listenerNameFilter.getReceivedEvents().get(4));

        assertEquals(event1, listenerBothFilters.getReceivedEvents().get(0));
        assertEquals(event2, listenerBothFilters.getReceivedEvents().get(1));
        assertEquals(event5, listenerBothFilters.getReceivedEvents().get(2));
    }

    /**
     * Test method for {@link org.dspace.services.events.SystemEventService#queueEvent(org.dspace.services.model.Event)}.
     */
    @Test
    public void testQueueEvent() {
        Event event1 = new Event("test.event.read", "test-resource-1", "11111", false);
        Event event2 = new Event("test.event.jump", null, "11111", false);
        Event event3 = new Event("some.event.write", "test-resource-2", "11111", true);
        Event event4 = new Event("some.event.add", "fake-resource-555", "11111", true);

        assertEquals(0, listenerNoFilter.getReceivedEvents().size());

        // no request, so it fires now
        eventService.queueEvent(event1);

        // now check that the listeners were properly triggered
        assertEquals(1, listenerNoFilter.getReceivedEvents().size());

        // now start a request
        requestService.startRequest();
        eventService.queueEvent(event2);
        eventService.queueEvent(event3);

        // not yet fired
        assertEquals(1, listenerNoFilter.getReceivedEvents().size());

        // fail request
        requestService.endRequest(new RuntimeException("die request!"));

        // still nothing because fail
        assertEquals(1, listenerNoFilter.getReceivedEvents().size());

        // try a successful one
        requestService.startRequest();
        eventService.queueEvent(event2);
        eventService.queueEvent(event3);
        eventService.queueEvent(event4);
        requestService.endRequest(null);
        
        assertEquals(4, listenerNoFilter.getReceivedEvents().size());
        assertEquals(event1, listenerNoFilter.getReceivedEvents().get(0));
        assertEquals(event2, listenerNoFilter.getReceivedEvents().get(1));
        assertEquals(event3, listenerNoFilter.getReceivedEvents().get(2));
        assertEquals(event4, listenerNoFilter.getReceivedEvents().get(3));
    }

    /**
     * Test method for {@link org.dspace.services.events.SystemEventService#registerEventListener(org.dspace.services.model.EventListener)}.
     */
    @Test
    public void testRegisterEventListener() {
        // check reregister ok
        eventService.registerEventListener(listenerNoFilter);
        eventService.registerEventListener(listenerNameFilter);
        eventService.registerEventListener(listenerBothFilters);

        // check null fails
        try {
            eventService.registerEventListener(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

}
