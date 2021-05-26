/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.sessions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.RequestInterceptor.RequestInterruptionException;
import org.dspace.services.sessions.model.HttpRequestImpl;
import org.dspace.services.sessions.model.InternalRequestImpl;
import org.dspace.utils.servicemanager.OrderedServiceComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the session service.
 * <p>
 * This depends on having something (a filter typically) which is
 * placing the current requests into a request storage cache.
 * <p>
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public final class StatelessRequestServiceImpl implements RequestService {

    private static final Logger log = LoggerFactory.getLogger(StatelessRequestServiceImpl.class);

    private ConfigurationService configurationService;

    @Autowired(required = true)
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * map for holding onto the request interceptors which is classloader safe.
     */
    private final Map<String, RequestInterceptor> interceptorsMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("init");
    }

    @PreDestroy
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
    @Override
    public String startRequest() {
        return startRequest(new InternalRequestImpl());
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#startRequest()
     */
    @Override
    public String startRequest(ServletRequest request, ServletResponse response) {
        return startRequest(new HttpRequestImpl(request, response));
    }

    private String startRequest(Request req) {
        // call the list of interceptors
        List<RequestInterceptor> interceptors = getInterceptors(false);
        for (RequestInterceptor requestInterceptor : interceptors) {
            if (requestInterceptor != null) {
                try {
                    requestInterceptor.onStart(req.getRequestId());
                } catch (RequestInterruptionException e) {
                    String message = "Request stopped from starting by exception from the interceptor (" +
                        requestInterceptor + "): " + e
                        .getMessage();
                    log.warn(message);
                    throw new RequestInterruptionException(message, e);
                } catch (Exception e) {
                    log.warn("Request interceptor (" + requestInterceptor + ") failed to execute on start (" + req
                        .getRequestId() + "): " + e.getMessage());
                }
            }
        }

        requests.setCurrent(req);
        return req.getRequestId();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#endRequest(java.lang.Exception)
     */
    @Override
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
            List<RequestInterceptor> interceptors = getInterceptors(true); // reverse
            for (RequestInterceptor requestInterceptor : interceptors) {
                if (requestInterceptor != null) {
                    try {
                        requestInterceptor.onEnd(requestId, (failure == null), failure);
                    } catch (RequestInterruptionException e) {
                        log.warn(
                            "Attempt to stop request from ending by an exception from the interceptor (" +
                                requestInterceptor + "), cannot stop requests from ending though so request end " +
                                "continues, this may be an error: " + e
                                .getMessage());
                    } catch (Exception e) {
                        log.warn(
                            "Request interceptor (" + requestInterceptor + ") failed to execute on end (" + requestId
                                + "): " + e
                                .getMessage());
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
        ArrayList<RequestInterceptor> l = new ArrayList<>(this.interceptorsMap.values());
        OrderedServiceComparator comparator = new OrderedServiceComparator();
        Collections.sort(l, comparator);
        if (reverse) {
            Collections.reverse(l);
        }
        return l;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#registerRequestListener(org.dspace.services.model.RequestInterceptor)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.services.RequestService#getCurrentUserId()
     */
    @Override
    public String getCurrentUserId() {
        Request currentRequest = getCurrentRequest();
        if (currentRequest == null) {
            return null;
        } else {
            return Objects.toString(currentRequest.getAttribute(AUTHENTICATED_EPERSON));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.services.RequestService#setCurrentUserId()
     */
    @Override
    public void setCurrentUserId(UUID epersonId) {
        Request currentRequest = getCurrentRequest();
        if (currentRequest != null) {
            getCurrentRequest().setAttribute(AUTHENTICATED_EPERSON, epersonId);
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.RequestService#getCurrentRequestId()
     */
    @Override
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
    @Override
    public Request getCurrentRequest() {
        return requests.getCurrent();
    }

    /**
     * Class to hold the current request. Uses Map keyed on current thread id.
     */
    private class RequestHolder {
        Map<Long, Request> requestMap = new ConcurrentHashMap<>();

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

    private final RequestHolder requests = new RequestHolder();
}
