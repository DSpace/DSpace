/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions;

import org.apache.commons.lang.StringUtils;
import org.dspace.kernel.mixins.InitializedService;
import org.dspace.kernel.mixins.ShutdownService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.SessionService;
import org.dspace.services.model.Request;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.RequestInterceptor.RequestInterruptionException;
import org.dspace.services.model.Session;
import org.dspace.services.sessions.model.HttpRequestImpl;
import org.dspace.services.sessions.model.InternalRequestImpl;
import org.dspace.utils.servicemanager.OrderedServiceComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implementation of the session service.
 * <p>
 * This depends on having something (a filter typically) which is 
 * placing the current requests into a request storage cache.
 * <p>
 * TODO use a HttpSessionListener to keep track of all sessions?
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class SessionRequestServiceImpl implements SessionService, RequestService, InitializedService, ShutdownService {

    private static Logger log = LoggerFactory.getLogger(SessionRequestServiceImpl.class);

    private ConfigurationService configurationService;
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * map for holding onto the request interceptors which is classloader safe.
     */
    private Map<String, RequestInterceptor> interceptorsMap = new HashMap<String, RequestInterceptor>();

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.InitializedService#init()
     */
    public void init() {
        log.info("init");
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ShutdownService#shutdown()
     */
    public void shutdown() {
        log.info("shutdown");
        clear();
    }

    /**
     * Clears out the settings inside this service.
     * Mostly for testing.
     */
    public void clear() {
        // immediately clear all interceptors when the service is terminated
        this.requests.clear();
        this.interceptorsMap.clear();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#startRequest()
     */
    public String startRequest() {
        return startRequest(new InternalRequestImpl());
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#startRequest()
     */
    public String startRequest(ServletRequest request, ServletResponse response) {
        return startRequest(new HttpRequestImpl(request, response));
    }

    private String startRequest(Request req) {
        // call the list of interceptors
        List<RequestInterceptor> interceptors = getInterceptors(false);
        for (RequestInterceptor requestInterceptor : interceptors) {
            if (requestInterceptor != null) {
                try {
                    requestInterceptor.onStart(req.getRequestId(), req.getSession());
                } catch (RequestInterruptionException e) {
                    String message = "Request stopped from starting by exception from the interceptor ("+requestInterceptor+"): " + e.getMessage();
                    log.warn(message);
                    throw new RequestInterruptionException(message, e);
                } catch (Exception e) {
                    log.warn("Request interceptor ("+requestInterceptor+") failed to execute on start ("+req.getRequestId()+"): " + e.getMessage());
                }
            }
        }

        requests.setCurrent(req);
        return req.getRequestId();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#endRequest(java.lang.Exception)
     */
    public String endRequest(Exception failure) {
        String requestId = null;
        try {
            requestId = getCurrentRequestId();
            if (StringUtils.isEmpty(requestId)) {
                // request not found, just log a warning
                log.debug("Attempting to end a request when none currently exists");
            } else {
                endRequest(requestId, failure);
            }
        } finally {
            requests.removeCurrent();
        }
        return requestId;
    }

    private void endRequest(String requestId, Exception failure) {
        if (requestId != null) {
            Session session = null;
            Request req = requests.get(requestId);
            if (req != null) {
                session = req.getSession();
            }
            
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
        }
    }

    /**
     * List this session's interceptors.
     *
     * @param reverse return the list in reverse order?
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
     * Makes a session from the existing HTTP session stuff in the 
     * current request, or creates a new session of non-HTTP related 
     * sessions.
     * 
     * @return the new session object which is placed into the request
     * @throws IllegalStateException if not session can be created
     */
    public Session getCurrentSession() {
        Request req = requests.getCurrent();
        if (req != null) {
            return req.getSession();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.SessionService#getCurrentSessionId()
     */
    public String getCurrentSessionId() {
        Request req = requests.getCurrent();
        if (req != null) {
            Session session = req.getSession();
            if (session != null) {
                return session.getSessionId();
            }
        }

        return null;
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
        Request req = requests.getCurrent();
        if (req != null) {
            return req.getRequestId();
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#getCurrentRequest()
     */
    public Request getCurrentRequest() {
        return requests.getCurrent();
    }

    /**
     * Class to hold the current request. Uses Map keyed on current thread id.
     */
    private class RequestHolder {
        Map<Long, Request> requestMap = new ConcurrentHashMap<Long, Request>();

        Request getCurrent() {
            return requestMap.get(Thread.currentThread().getId());
        }

        void setCurrent(Request req) {
            requestMap.put(Thread.currentThread().getId(), req);
        }

        void removeCurrent() {
            requestMap.remove(Thread.currentThread().getId());
        }

        Request get(String requestId) {
            if (!StringUtils.isEmpty(requestId)) {
                for (Request req : requestMap.values()) {
                    if (req != null && requestId.equals(req.getRequestId())) {
                        return req;
                    }
                }
            }

            return null;
        }
        void remove(String requestId) {
            if (!StringUtils.isEmpty(requestId)) {
                for (Map.Entry<Long, Request> reqEntry : requestMap.entrySet()) {
                    if (reqEntry.getValue() != null && requestId.equals(reqEntry.getValue().getRequestId())) {
                        requestMap.remove(reqEntry.getKey());
                    }
                }
            }
        }

        void clear() {
            for (Request request : requestMap.values()) {
                try {
                    endRequest(request.getRequestId(), null);
                } catch (RuntimeException e) {
                    log.error("Runtime exception ending request", e);
                } catch (Exception e) {
                    log.error("Exception ending request", e);
                }
            }

            requestMap.clear();
        }
    }

    private RequestHolder requests = new RequestHolder();
}
