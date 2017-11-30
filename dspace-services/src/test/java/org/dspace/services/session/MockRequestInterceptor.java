/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.session;

import org.dspace.services.model.RequestInterceptor;
import org.dspace.services.model.Session;


/**
 * This is a mock request interceptor for testing
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class MockRequestInterceptor implements RequestInterceptor {

    public String state = "";
    public int hits = 0;

    /* (non-Javadoc)
     * @see org.dspace.services.model.RequestInterceptor#onEnd(java.lang.String, org.dspace.services.model.Session, boolean, java.lang.Exception)
     */
    public void onEnd(String requestId, Session session, boolean succeeded, Exception failure) {
        if (succeeded) {
            state = "end:success:" + requestId;
        } else {
            state = "end:fail:" + requestId;
        }
        hits++;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.RequestInterceptor#onStart(java.lang.String, org.dspace.services.model.Session)
     */
    public void onStart(String requestId, Session session) {
        state = "start:" + requestId;
        hits++;
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.OrderedService#getOrder()
     */
    public int getOrder() {
        return 10;
    }

}
