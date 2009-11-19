/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
        String cacheRID = (String) getRequestCache().get(CachingService.REQUEST_ID_KEY);
        assertNotNull(cacheRID);
        assertEquals(cacheRID, requestId);

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
        String cacheRID = (String) getRequestCache().get(CachingService.REQUEST_ID_KEY);
        assertNull(cacheRID);
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
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#makeSession(java.lang.String)}.
     */
    @Test
    public void testMakeSession() {
        Session session = sessionRequestService.makeSession("AZ-session");
        assertNotNull(session);
        assertEquals("AZ-session", session.getId());
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getSession(java.lang.String)}.
     */
    @Test
    public void testGetSession() {
        Session s = sessionRequestService.getSession("aaronz");
        assertNull(s);
 
        Session newOne = sessionRequestService.makeSession("aaronz");

        s = sessionRequestService.getSession("aaronz");
        assertNotNull(s);
        assertEquals(newOne, s);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getAllActiveSessions()}.
     */
    @Test
    public void testGetAllActiveSessions() {
        List<Session> l = sessionRequestService.getAllActiveSessions();
        assertNotNull(l);
        assertEquals(0, l.size());

        Session newOne = sessionRequestService.makeSession("aaronz");
        Session newTwo = sessionRequestService.makeSession("beckyz");

        l = sessionRequestService.getAllActiveSessions();
        assertNotNull(l);
        assertEquals(2, l.size());

        newOne.invalidate();

        l = sessionRequestService.getAllActiveSessions();
        assertNotNull(l);
        assertEquals(1, l.size());

        newTwo.clear();
        
        l = sessionRequestService.getAllActiveSessions();
        assertNotNull(l);
        assertEquals(1, l.size());

        newTwo.invalidate();

        l = sessionRequestService.getAllActiveSessions();
        assertNotNull(l);
        assertEquals(0, l.size());
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#bindSession(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testBindSession() {
        Session session = null;

        try {
            sessionRequestService.bindSession("AZ1", "/user/aaronz", "aaronz");
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }

        sessionRequestService.makeSession("AZ1");
        
        session = sessionRequestService.bindSession("AZ1", "/user/aaronz", "aaronz");
        assertNotNull(session);
        assertEquals("AZ1", session.getId());
        assertEquals("/user/aaronz", session.getUserId());

        session = sessionRequestService.bindSession("AZ1", null, null);
        assertNotNull(session);
        assertEquals("AZ1", session.getId());
        assertEquals(null, session.getUserId());
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#startSession(java.lang.String)}.
     */
    @Test
    public void testStartSession() {
        Session session = sessionRequestService.startSession("aaronz");
        assertNotNull(session);
        assertEquals("aaronz", session.getId());
        session.invalidate();

        // try making one with null
        Session s2 = sessionRequestService.startSession(null);
        assertNotNull(s2);
        assertNotNull(s2.getId());
        s2.invalidate();
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentSession()}.
     */
    @Test
    public void testGetCurrentSession() {
        Session current = sessionRequestService.getCurrentSession();
        assertNull(current);

        Session session = sessionRequestService.startSession("aaronz");
        assertNotNull(session);
        assertEquals("aaronz", session.getId());

        current = sessionRequestService.getCurrentSession();
        assertNotNull(current);
        assertEquals("aaronz", current.getId());
        assertEquals(current, session);
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentSessionId()}.
     */
    @Test
    public void testGetCurrentSessionId() {
        String current = sessionRequestService.getCurrentSessionId();
        assertNull(current);

        Session session = sessionRequestService.startSession("aaronz");
        assertNotNull(session);
        assertEquals("aaronz", session.getId());

        current = sessionRequestService.getCurrentSessionId();
        assertNotNull(current);
        assertEquals("aaronz", current);
        assertEquals(current, session.getId());
    }

    /**
     * Test method for {@link org.dspace.services.sessions.SessionRequestServiceImpl#getCurrentUserId()}.
     */
    @Test
    public void testGetCurrentUserId() {
        String current = sessionRequestService.getCurrentUserId();
        assertNull(current);

        Session session = sessionRequestService.startSession("aaronz");
        assertNotNull(session);
        assertEquals("aaronz", session.getId());
        assertEquals(null, session.getUserId());

        current = sessionRequestService.getCurrentUserId();
        assertNull(current);

        sessionRequestService.bindSession(session.getId(), "/user/aaronz", "aaronz");

        current = sessionRequestService.getCurrentUserId();
        assertNotNull(current);
        assertEquals("/user/aaronz", current);
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
        Cache cache = cachingService.getCache(CachingService.REQUEST_CACHE, new CacheConfig(CacheScope.REQUEST));
        return cache;
    }

}
