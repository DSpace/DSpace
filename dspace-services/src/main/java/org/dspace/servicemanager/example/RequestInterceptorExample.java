/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.RequestService;
import org.dspace.services.model.RequestInterceptor;

/**
 * A sample RequestInterceptor which simply logs request start and end
 * calls.
 *
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public final class RequestInterceptorExample implements RequestInterceptor {

    private static final Logger log = LogManager.getLogger();

    /**
     * Constructor which will inject the instantiated
     * Interceptor into a service handed to it.
     *
     * @param service the service
     */
    public RequestInterceptorExample(RequestService service) {
        service.registerRequestInterceptor(this);
    }

    @Override
    public void onEnd(String requestId, boolean succeeded,
                      Exception failure) {
        log.info("Intercepting End of Request: id={}, succeeded={}", requestId, succeeded);
    }

    @Override
    public void onStart(String requestId) {
        log.info("Intercepting Start of Request: id={}", requestId);
    }

    @Override
    public int getOrder() {
        // TODO Auto-generated method stub
        return 0;
    }

}
