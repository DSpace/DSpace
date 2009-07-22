/**
 * $Id: RequestService.java 3285 2008-11-13 18:30:54Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/RequestService.java $
 * RequestService.java - DSpace2 - Oct 14, 2008 4:59:22 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services;

import org.dspace.services.model.RequestInterceptor;

/**
 * Allows for the managing of requests in the system in a way which is independent
 * from any underlying system or code
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface RequestService {

    /**
     * Initiates a request in the system,
     * normally this would be triggered by a servlet request starting <br/>
     * Only one request can be associated with the current thread so if another one is running it will be
     * destroyed and a new one will be created <br/>
     * Note that requests are expected to be manually ended somehow and will not be closed out automatically
     * 
     * @return the unique generated id for the new request
     * @throws IllegalArgumentException if the session is null, invalid, or there is no current session
     */
    public String startRequest();

    /**
     * Ends the current running request, this can indicate success or failure of the request,
     * this will trigger the interceptors and normally would be caused by a servlet request ending,
     * note that a request cannot be ended twice, once it is ended this will just return null
     * 
     * @param failure (optional) this is the exception associated with the failure, 
     * leave as null if the request is ending successfully (you can make up a {@link RuntimeException}
     * if you just need to indicate that the request failed)
     * @return the request ID if the request closes successfully and is not already closed OR null if there is no current request
     */
    public String endRequest(Exception failure);

    /**
     * Finds out of there is a request running in this thread and if so what the id of that request is
     * 
     * @return the id of the current request for this thread OR null if there is not one
     */
    public String getCurrentRequestId();

    /**
     * Allows developers to perform actions on the start and end of the request cycle,
     * if you decide you do not need to use your interceptor anymore then simply destroy
     * it (dereference it) and the service will stop calling it, 
     * along those lines you should
     * not register an interceptor that you do not keep a reference to (like an inline class
     * or registerRequestListener(new YourInterceptor())) as this will be destroyed immediately,
     * this registration is ClassLoader safe
     * 
     * @param interceptor an implementation of {@link RequestInterceptor}
     * @throws IllegalArgumentException if this priority is invalid or the input is null
     */
    public void registerRequestInterceptor(RequestInterceptor interceptor);

}
