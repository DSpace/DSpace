/**
 * $Id: MockRequestInterceptor.java 3251 2008-10-29 17:15:31Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/services/session/MockRequestInterceptor.java $
 * MockRequestInterceptor.java - DSpace2 - Oct 29, 2008 4:55:59 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
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
