/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.UUID;

import org.dspace.services.model.Request;
import org.dspace.services.model.RequestInterceptor;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Allows for the managing of requests in the system in a way which is 
 * independent of any underlying system or code.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface RequestService {

    /**
     * Request attribute name for the current authenticated user
     */
    static final String AUTHENTICATED_EPERSON = "authenticated_eperson";

    /**
     * Initiates a request in the system.
     * Normally this would be triggered by a servlet request starting.
     * <p>
     * Only one request can be associated with the current thread, so if 
     * another one is running it will be destroyed and a new one will be 
     * created.
     * <p>
     * Note that requests are expected to be manually ended somehow and 
     * will not be closed out automatically.
     * 
     * @return the unique generated id for the new request
     * @throws IllegalArgumentException if the session is null, invalid, or there is no current session
     */
    public String startRequest();

    /**
     * Initiates a request in the system,
     * normally this would be triggered by a servlet request starting <br>
     * Only one request can be associated with the current thread so if another one is running it will be
     * destroyed and a new one will be created <br>
     * Note that requests are expected to be manually ended somehow and will not be closed out automatically
     *
     * @param request servlet request
     * @param response servlet response
     * @return the unique generated id for the new request
     * @throws IllegalArgumentException if the session is null, invalid, or there is no current session
     */
    public String startRequest(ServletRequest request, ServletResponse response);

    /**
     * Ends the current running request, this can indicate success or failure of the request,
     * this will trigger the interceptors and normally would be caused by a servlet request ending,
     * note that a request cannot be ended twice, once it is ended this will just return null
     * 
     * @param failure (optional) this is the exception associated with 
     * the failure.  Leave as null if the request is ending successfully.
     * You can make up a {@link RuntimeException} if you just need to 
     * indicate that the request failed.
     * @return the request ID if the request closes successfully and is 
     * not already closed OR null if there is no current request.
     */
    public String endRequest(Exception failure);

    /**
     * Finds out of there is a request running in this thread and if so 
     * what the id of that request is.
     * 
     * @return the id of the current request for this thread OR null if there is not one
     */
    public String getCurrentRequestId();

    /**
     * Finds out of there is a request running in this thread and if so returns it
     *
     * @return the current request for this thread OR null if there is not one
     */
    public Request getCurrentRequest();

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

    /**
     * Access the current user id for the current session.
     * (also available from the current session)
     *
     * @return the id of the user associated with the current thread OR null if there is no user
     */
    public String getCurrentUserId();

    /**
     * Set the ID of the current authenticated user
     *
     * @return the id of the user associated with the current thread OR null if there is no user
     */
    public void setCurrentUserId(UUID epersonId);

}
