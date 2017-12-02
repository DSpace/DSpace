/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.session;

import org.dspace.services.CachingService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.services.sessions.StatelessRequestServiceImpl;
import org.dspace.test.DSpaceAbstractKernelTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Testing the request and session services
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class StatelessRequestServiceImplTest extends DSpaceAbstractKernelTest {

    private StatelessRequestServiceImpl statelessRequestService;
    private CachingService cachingService; 

    @Before
    public void before() {
        statelessRequestService = getService(StatelessRequestServiceImpl.class);
        cachingService = getService(CachingService.class);
    }

    @After
    public void after() {
        statelessRequestService.clear();
        cachingService.resetCaches();
        statelessRequestService = null;
        cachingService = null;
    }

    /**
     * Test method for {@link org.dspace.services.sessions.StatelessRequestServiceImpl#startRequest()}.
     */
    @Test
    public void testStartRequest() {
        String requestId = statelessRequestService.startRequest();
        assertNotNull(requestId);

        statelessRequestService.endRequest(null);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.StatelessRequestServiceImpl#endRequest(java.lang.Exception)}.
     */
    @Test
    public void testEndRequest() {
        String requestId = statelessRequestService.startRequest();
        assertNotNull(requestId);

        statelessRequestService.endRequest(null);
        assertNull( getRequestCache() );
    }

    /**
     * Test method for {@link org.dspace.services.sessions.StatelessRequestServiceImpl#registerRequestInterceptor(org.dspace.services.model.RequestInterceptor)}.
     */
    @Test
    public void testRegisterRequestListener() {
        MockRequestInterceptor mri = new MockRequestInterceptor();
        statelessRequestService.registerRequestInterceptor(mri);
        assertEquals("", mri.state);
        assertEquals(0, mri.hits);

        String requestId = statelessRequestService.startRequest();
        assertEquals(1, mri.hits);
        assertTrue( mri.state.startsWith("start") );
        assertTrue( mri.state.contains(requestId));

        statelessRequestService.endRequest(null);
        assertEquals(2, mri.hits);
        assertTrue( mri.state.startsWith("end") );
        assertTrue( mri.state.contains("success"));
        assertTrue( mri.state.contains(requestId));

        requestId = statelessRequestService.startRequest();
        assertEquals(3, mri.hits);
        assertTrue( mri.state.startsWith("start") );
        assertTrue( mri.state.contains(requestId));

        statelessRequestService.endRequest( new RuntimeException("Oh Noes!") );
        assertEquals(4, mri.hits);
        assertTrue( mri.state.startsWith("end") );
        assertTrue( mri.state.contains("fail"));
        assertTrue( mri.state.contains(requestId));

        try {
            statelessRequestService.registerRequestInterceptor(null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Test method for {@link org.dspace.services.sessions.StatelessRequestServiceImpl#getCurrentUserId()}.
     */
    @Test
    public void testGetCurrentUserId() {
        String current = statelessRequestService.getCurrentUserId();
        assertNull(current);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.StatelessRequestServiceImpl#getCurrentRequestId()}.
     */
    @Test
    public void testGetCurrentRequestId() {
        String requestId = statelessRequestService.getCurrentRequestId();
        assertNull(requestId); // no request yet

        String rid = statelessRequestService.startRequest();

        requestId = statelessRequestService.getCurrentRequestId();
        assertNotNull(requestId);
        assertEquals(rid, requestId);

        statelessRequestService.endRequest(null);

        requestId = statelessRequestService.getCurrentRequestId();
        assertNull(requestId); // no request yet
    }

    
    /**
     * @return the request storage cache
     */
    private Cache getRequestCache() {
        return cachingService.getCache(CachingService.REQUEST_CACHE, new CacheConfig(CacheScope.REQUEST));
    }

}
