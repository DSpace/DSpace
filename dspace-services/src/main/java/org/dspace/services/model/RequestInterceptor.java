/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.model;

import org.dspace.kernel.mixins.OrderedService;

/**
 * Executes at the beginning and/or end of system requests.
 * <p>
 * Note that the ordering of these must be set specifically higher than 
 * 0 or an exception will occur.  If you do not really care what the
 * order is you are encouraged to use 10.
 * <p>
 * Note that the highest priority interceptor will be executed first in
 * request start and last on request end. The lowest priority
 * interceptor would be executed last on request start and first on
 * request end. If you need an interceptor which can execute first on
 * request start and first on request end (or vice versa) then create 2 
 * interceptors. :-)
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface RequestInterceptor extends OrderedService {

    /**
     * Take actions before the request is handled for an operation.
     * This will be called just before each request is sent to the
     * correct request handler.  For example: starting a transaction 
     * would happen at this point.
     * <p>
     * If you want to interrupt the handling of this request (stop it)
     * then throw a {@link RequestInterruptionException}.
     * 
     * @param requestId the unique id of the request
     * @throws RequestInterruptionException if this interceptor wants to stop the request
     */
    public void onStart(String requestId);

    /**
     * Take actions after the request is handled for an operation.
     * This will be called just before each operation is totally
     * completed.  For example: closing a transaction would happen at
     * this point.
     * <p>
     * If you want to interrupt the handling of this request (stop it)
     * then throw a {@link RequestInterruptionException}.
     * <p>
     * <b>NOTE:</b>  it is important to realize that this will be called
     * even if the request fails.  Please check the incoming success
     * param to see if this request was successful or not.  This is your
     * cue to rollback or commit, for example.
     * 
     * @param requestId the unique id of the request
     * @param succeeded true if the request operations were successful, false if there was a failure
     * @param failure this is the exception associated with the failure, it is null if there is no associated exception
     */
    public void onEnd(String requestId, boolean succeeded, Exception failure);

    /**
     * Indicate that request processing should be halted.  This should 
     * only be used in extreme cases as it will halt all request
     * processing and cause any remaining interceptors to be skipped.
     * A message about the halt should be placed into the message field.
     */
    public static class RequestInterruptionException extends RuntimeException {
    	private static final long serialVersionUID = 1L;
        public RequestInterruptionException(String message, Throwable cause) {
            super(message, cause);
        }
        public RequestInterruptionException(String message) {
            super(message);
        }
    }

}
