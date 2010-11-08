/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.session;

import static org.junit.Assert.*;

import java.util.List;

import org.dspace.services.CachingService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.Session;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.services.sessions.SessionRequestServiceImpl;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Testing the request and session services
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class SessionRequestServiceImplTest extends DSpaceAbstractKernelTest {

    private SessionRequestServiceImpl sessionRequestService; 
    private CachingService cachingService; 

    @Before
    public void before() {
        sessionRequestService = getService(SessionRequestServiceImpl.class);
        cachingService = getService(CachingService.class);
    }

    @After
    public void after() {
        sessionRequestService.clear();
        cachingService.resetCaches();
        sessionRequestService = null;
        cachingService = null;
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#startRequest()}.
     */
    @Test
    public void testStartRequest() {
        String requestId = sessionRequestService.startRequest();
        assertNotNull(requestId);

        sessionRequestService.endRequest(null);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#endRequest(java.lang.Exception)}.
     */
    @Test
    public void testEndRequest() {
        String requestId = sessionRequestService.startRequest();
        assertNotNull(requestId);

        sessionRequestService.endRequest(null);
        assertNull( getRequestCache() );
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#registerRequestInterceptor(org.dspace.services.model.RequestInterceptor)}.
     */
    @Test
    public void testRegisterRequestListener() {
        MockRequestInterceptor mri = new MockRequestInterceptor();
        sessionRequestService.registerRequestInterceptor(mri);
        assertEquals("", mri.state);
        assertEquals(0, mri.hits);

        String requestId = sessionRequestService.startRequest();
        assertEquals(1, mri.hits);
        assertTrue( mri.state.startsWith("start") );
        assertTrue( mri.state.contains(requestId));

        sessionRequestService.endRequest(null);
        assertEquals(2, mri.hits);
        assertTrue( mri.state.startsWith("end") );
        assertTrue( mri.state.contains("success"));
        assertTrue( mri.state.contains(requestId));

        requestId = sessionRequestService.startRequest();
        assertEquals(3, mri.hits);
        assertTrue( mri.state.startsWith("start") );
        assertTrue( mri.state.contains(requestId));

        sessionRequestService.endRequest( new RuntimeException("Oh Noes!") );
        assertEquals(4, mri.hits);
        assertTrue( mri.state.startsWith("end") );
        assertTrue( mri.state.contains("fail"));
        assertTrue( mri.state.contains(requestId));

        try {
            sessionRequestService.registerRequestInterceptor(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentSession()}.
     */
    @Test
    public void testGetCurrentSession() {
        Session current = sessionRequestService.getCurrentSession();
        assertNull(current);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentSessionId()}.
     */
    @Test
    public void testGetCurrentSessionId() {
        String current = sessionRequestService.getCurrentSessionId();
        assertNull(current);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentUserId()}.
     */
    @Test
    public void testGetCurrentUserId() {
        String current = sessionRequestService.getCurrentUserId();
        assertNull(current);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentRequestId()}.
     */
    @Test
    public void testGetCurrentRequestId() {
        String requestId = sessionRequestService.getCurrentRequestId();
        assertNull(requestId); // no request yet

        String rid = sessionRequestService.startRequest();

        requestId = sessionRequestService.getCurrentRequestId();
        assertNotNull(requestId);
        assertEquals(rid, requestId);

        sessionRequestService.endRequest(null);

        requestId = sessionRequestService.getCurrentRequestId();
        assertNull(requestId); // no request yet
    }

    
    /**
     * @return the request storage cache
     */
    private Cache getRequestCache() {
        return cachingService.getCache(CachingService.REQUEST_CACHE, new CacheConfig(CacheScope.REQUEST));
    }

}
