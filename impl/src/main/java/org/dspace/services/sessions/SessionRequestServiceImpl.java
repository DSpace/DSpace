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
package org.dspace.services.sessions;

import org.azeckoski.reflectutils.refmap.ReferenceMap;
import org.azeckoski.reflectutils.refmap.ReferenceType;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.services.CachingService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.SessionService;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.RequestInterceptor.RequestInterruptionException;
import org.dspace.services.model.Session;
import org.dspace.services.sessions.model.SessionImpl;
import org.dspace.utils.servicemanager.OrderedServiceComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Implementation of the session service <br/>
 * This depends on having something (a filter typically) which is placing the current requests into
 * a request storage cache <br/>
 * TODO use a HttpSessionListener to keep track of all sessions?
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class SessionRequestServiceImpl implements SessionService, RequestService, InitializedService, ShutdownService {

    private static Logger log = LoggerFactory.getLogger(SessionRequestServiceImpl.class);

    public static final String REQUEST_ID_PREFIX = "request-";

    private CachingService cachingService;
    @Autowired
    @Required
    public void setCachingService(CachingService cachingService) {
        this.cachingService = cachingService;
    }

    private ConfigurationService configurationService;
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * map for holding onto the request interceptors which is classloader safe
     */
    private ReferenceMap<String, RequestInterceptor> interceptorsMap = new ReferenceMap<String, RequestInterceptor>(ReferenceType.STRONG, ReferenceType.WEAK);

    /**
     * Keeps track of the sessions created by this service
     */
    private ConcurrentHashMap<String, SessionImpl> sessions = new ConcurrentHashMap<String, SessionImpl>();

    private ScheduledExecutorService expiryService = null;

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.InitializedService#init()
     */
    public void init() {
        log.info("init");

        // start up the session expiring timer
        expiryService = new ScheduledThreadPoolExecutor(1);

        // start up the session expiry thread which runs every 5 minutes
        long expiryInterval = configurationService.getPropertyAsType("session.expiry.interval", 300000l);
        expiryService.scheduleAtFixedRate(new SessionCleanup(), expiryInterval, expiryInterval, TimeUnit.MILLISECONDS);
    }

    protected class SessionCleanup implements Runnable {
        public void run() {
            int expiredCount = 0;
            for (Iterator<SessionImpl> iterator = sessions.values().iterator(); iterator.hasNext();) {
                SessionImpl session = iterator.next();
                if (session.isInvalidated()) {
                    // already invalidated so trash it
                    iterator.remove();
                    expiredCount++;
                } else {
                    // time to invalidate it so do that and then trash it
                    long mii = session.getMaxInactiveInterval();
                    long lat = session.getLastAccessedTime();
                    long curInterval = System.currentTimeMillis() - lat;
                    if (curInterval > (mii * 1000l)) {
                        session.invalidate();
                        iterator.remove();
                        expiredCount++;
                    }
                }
            }
            log.info("Expired "+expiredCount+" user sessions, "+sessions.size()+" sessions remain active");
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ShutdownService#shutdown()
     */
    public void shutdown() {
        log.info("shutdown");
        clear();

        // Cancel the Timer on shutdown
        if (this.expiryService != null) {
            List<Runnable> scheduledTasks = this.expiryService.shutdownNow();
            if (scheduledTasks != null && scheduledTasks.size() > 0) {
                log.info("Shutdown with " + scheduledTasks.size() + " tasks remaining");
            }
        }
    }

    /**
     * clears out the settings inside this service,
     * mostly for testing
     */
    public void clear() {
        // immediately clear all interceptors when the service is terminated
        this.interceptorsMap.clear();
        // also clear the entire copy of all running sessions when this shuts down
        this.sessions.clear();
        // flush the request cache
        if (this.cachingService != null) {
            this.cachingService.unbindRequestCaches();
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#startRequest()
     */
    public String startRequest() {
        // assure a session exists in the request cache before the request starts
        Session session = makeSession(null);
        // generate a requestId
        String requestId = makeRequestId();
        // call the list of interceptors
        List<RequestInterceptor> interceptors = getInterceptors(false);
        for (RequestInterceptor requestInterceptor : interceptors) {
            if (requestInterceptor != null) {
                try {
                    requestInterceptor.onStart(requestId, session);
                } catch (RequestInterruptionException e) {
                    String message = "Request stopped from starting by exception from the interceptor ("+requestInterceptor+"): " + e.getMessage();
                    log.warn(message);
                    throw new RequestInterruptionException(message, e);
                } catch (Exception e) {
                    log.warn("Request interceptor ("+requestInterceptor+") failed to execute on start ("+requestId+"): " + e.getMessage());
                }
            }
        }
        // put the id into the request cache
        getRequestCache().put(CachingService.REQUEST_ID_KEY, requestId);
        return requestId;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#endRequest(java.lang.Exception)
     */
    public String endRequest(Exception failure) {
        String requestId = getCurrentRequestId();
        if (requestId != null) {
            try {
                getRequestCache().remove(CachingService.REQUEST_ID_KEY);
                // get session and execute the interceptors
                Session session = getCurrentSession();
                List<RequestInterceptor> interceptors = getInterceptors(true); // reverse
                for (RequestInterceptor requestInterceptor : interceptors) {
                    if (requestInterceptor != null) {
                        try {
                            requestInterceptor.onEnd(requestId, session, (failure == null), failure);
                        } catch (RequestInterruptionException e) {
                            log.warn("Attempt to stop request from ending by an exception from the interceptor ("+requestInterceptor+"), cannot stop requests from ending though so request end continues, this may be an error: " + e.getMessage());
                        } catch (Exception e) {
                            log.warn("Request interceptor ("+requestInterceptor+") failed to execute on end ("+requestId+"): " + e.getMessage());
                        }
                    }
                }
            } finally {
                // purge the request caches
                cachingService.unbindRequestCaches();
            }
        } else {
            // request not found, just log a warning
            log.debug("Attempting to end a request when none currently exists");
        }
        return requestId;
    }

    private Random random = new Random();
    /**
     * @return a generated request Id used to identify and track this request
     */
    private String makeRequestId() {
        String requestId = REQUEST_ID_PREFIX + random.nextInt(1000) + "-" + System.currentTimeMillis();
        Session session = getCurrentSession();
        if (session != null) {
            requestId += ":" + session.getId() + ":" + session.getUserId();
        }
        return requestId;
    }

    /**
     * @return the current list of interceptors in the correct order
     */
    private List<RequestInterceptor> getInterceptors(boolean reverse) {
        ArrayList<RequestInterceptor> l = new ArrayList<RequestInterceptor>( this.interceptorsMap.values() );
        OrderedServiceComparator comparator = new OrderedServiceComparator();
        Collections.sort(l, comparator );
        if (reverse) {
            Collections.reverse(l);
        }
        return l;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#registerRequestListener(org.dspace.services.model.RequestInterceptor)
     */
    public void registerRequestInterceptor(RequestInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("Cannot register an interceptor that is null");
        }
        if (interceptor.getOrder() <= 0) {
            throw new IllegalArgumentException("Interceptor ordering for RequestInterceptor's must be greater than 0");
        }
        String key = interceptor.getOrder() + ":" + interceptor.getClass().getName();
        this.interceptorsMap.put(key, interceptor);
    }

    /**
     * Makes a session from the existing http session stuff in the current request or creates
     * a new session of non-http related sessions
     * 
     * @param sessionId an optional id to assign
     * @return the new session object which is placed into the request
     * @throws IllegalStateException if not session can be created
     */
    public Session makeSession(String sessionId) {
        SessionImpl sessionImpl = null;
        Cache cache = getRequestCache();
        if (sessionId == null) {
            // gets the current session from the request if one exists
            String curSessionId = getCurrentSessionId();
            sessionImpl = getSessionImpl(curSessionId);
        }
        if (sessionImpl == null) {
            // no session found yet so make one
            HttpServletRequest httpRequest = (HttpServletRequest) cache.get(CachingService.HTTP_REQUEST_KEY);
            if (httpRequest == null) {
                // creates the session in a local storage
                sessionImpl = new SessionImpl();
            } else {
                // create a session from the one in the request
                sessionImpl = new SessionImpl(httpRequest);
            }
            if (sessionImpl.isIncomplete()) {
                // set the required values on the session
                sessionImpl.setSessionId(sessionId);
                String serverId = configurationService.getProperty("server.id");
                sessionImpl.setServerId(serverId);
                int interval = configurationService.getPropertyAsType("session.max.inactive.interval", 3600);
                sessionImpl.setMaxInactiveInterval(interval);
                log.info("Created new session: " + sessionImpl);
            }
            // put the session in the request cache
            cache.put(CachingService.SESSION_ID_KEY, sessionImpl.getId());
        }
        // track the session in the map (or refresh it)
        sessions.put(sessionImpl.getSessionId(), sessionImpl); // sessions mapped by the sessionId and not the id
        return sessionImpl;
    }

    /**
     * Retrieves a session by the id if it is active
     * 
     * @param sessionId the unique id for a session
     * @return a session if one is available OR null if none found
     * @throws IllegalArgumentException if the sessionId is null
     */
    public Session getSession(String sessionId) {
        return getSessionImpl(sessionId);
    }

    /**
     * INTERNAL USE
     * Same as getSession but retrieves the implementation
     */
    private SessionImpl getSessionImpl(String sessionId) {
        SessionImpl session = null;
        if (sessionId != null) {
            session = sessions.get(sessionId);
            if (session != null) {
                if (session.isInvalidated()) {
                    sessions.remove(sessionId);
                    session = null;
                }
            }
        }
        return session;
    }

    /**
     * Get the list of sessions,
     * this will automatically purge out any sessions which have expired
     * @return the list of all active sessions ordered by last time accessed
     */
    public List<Session> getAllActiveSessions() {
        ArrayList<Session> l = new ArrayList<Session>();
        for (Iterator<SessionImpl> iterator = sessions.values().iterator(); iterator.hasNext();) {
            SessionImpl session = iterator.next();
            if (session.isInvalidated()) {
                iterator.remove();
            } else {
                l.add(session);
            }
        }
        Collections.sort(l, new SessionImpl.SessionLastAccessedComparator());
        return l;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#bindSession(java.lang.String, java.lang.String, java.lang.String)
     */
    public Session bindSession(String sessionId, String userId, String userEid) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId cannot be null");
        }
        SessionImpl session = getSessionImpl(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Could not find a session with the id: " + sessionId);
        } else {
            if (userId != null) {
                if (userEid == null || "".equals(userEid)) {
                    throw new IllegalArgumentException("userEid must be set when userId is set or cannot bind the session");
                }
                session.setUserId(userId, userEid);
            } else {
                session.setUserId(null, null);
            }
        }
        return session;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#startSession(java.lang.String)
     */
    public Session startSession(String sessionId) {
        return makeSession(sessionId);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#getCurrentSession()
     */
    public Session getCurrentSession() {
        String sessionId = getCurrentSessionId();
        Session session = getSessionImpl(sessionId);
        return session;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#getCurrentSessionId()
     */
    public String getCurrentSessionId() {
        String sessionId = (String) getRequestCache().get(CachingService.SESSION_ID_KEY);
        return sessionId;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#getCurrentUserId()
     */
    public String getCurrentUserId() {
        String userId = null;
        Session session = getCurrentSession();
        if (session != null) {
            userId = session.getUserId();
        }
        return userId;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#getCurrentRequestId()
     */
    public String getCurrentRequestId() {
        String requestId = (String) getRequestCache().get(CachingService.REQUEST_ID_KEY);
        return requestId;
    }


    /**
     * @return the request storage cache
     */
    private Cache getRequestCache() {
        Cache cache = cachingService.getCache(CachingService.REQUEST_CACHE, new CacheConfig(CacheScope.REQUEST));
        return cache;
    }

}
