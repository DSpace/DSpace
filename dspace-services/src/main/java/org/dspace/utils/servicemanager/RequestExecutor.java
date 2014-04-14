/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;

import java.lang.ref.WeakReference;

import org.dspace.services.RequestService;
import org.dspace.services.model.RequestInterceptor;


/**
 * This will execute a request and ensure that it closes even if the 
 * thing that is being executed (run) dies.  It will appropriately end 
 * the request based on the result of the execution and can start a new 
 * request or tie into an existing request if there is one.
 * <p>
 * This is also a Runnable so it can be used as a wrapper which allows 
 * something to be executed inside a request.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class RequestExecutor implements Runnable {

    private final WeakReference<RequestService> requestServiceRef;
    private final Runnable toExecute;
    private final boolean useExistingRequestIfPossible;

    /**
     * Create an executor which can be used to execute the runnable 
     * within a request by calling the execute method.
     * This will create a new request to execute the code in when the 
     * execute method is called.
     * All parameters are required.
     * 
     * @param requestService the request service
     * @param toExecute the code to execute
     * @throws IllegalArgumentException if the params are null
     */
    public RequestExecutor(RequestService requestService, Runnable toExecute) {
        this(requestService, toExecute, false);
    }

    /**
     * Create an executor which can be used to execute the runnable 
     * within a request by calling the execute method.
     * All parameters are required.
     * 
     * @param requestService the request service
     * @param toExecute the code to execute
     * @param useExistingRequestIfPossible if true then this will try to attach to an existing request and
     * will create a new request if none is found, if false it will create a new request to execute in
     * @throws IllegalArgumentException if the params are null
     */
    public RequestExecutor(RequestService requestService, Runnable toExecute, boolean useExistingRequestIfPossible) {
        if (toExecute == null || requestService == null) {
            throw new IllegalArgumentException("toExecute and requestService must both be set (neither can be null)");
        }
        this.requestServiceRef = new WeakReference<RequestService>(requestService);
        this.toExecute = toExecute;
        this.useExistingRequestIfPossible = useExistingRequestIfPossible;
    }

    /**
     * Execute the {@link Runnable} which is contained in this object,
     * the same as calling {@link #run()}.
     * @throws RequestInterceptor.RequestInterruptionException if the method fails
     */
    public void execute() {
        RequestService requestService = this.requestServiceRef.get();
        if (requestService == null) {
            throw new IllegalStateException("it is no longer possible to execute this because the RequestService is no longer valid");
        }
        String requestId = null;
        if (useExistingRequestIfPossible) {
            requestId = requestService.getCurrentRequestId();
        }
        boolean newRequest = false;
        if (requestId == null) {
            // start new request
            requestId = requestService.startRequest();
            newRequest = true;
        }
        try {
            this.toExecute.run();
            if (newRequest) {
                // end the request here if it was a new one
                requestService.endRequest(null);
            }
        } catch (Exception e) {
            requestService.endRequest(e);
            throw new RequestInterceptor.RequestInterruptionException("Failure during execution of Runnable (" + toExecute + ") in request ("+requestId+"):" + e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        execute();
    }

}
