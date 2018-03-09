/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

import org.apache.log4j.Logger;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.exception.SourceExceptionHandler;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class contains primitives to handle request timeouts and to retry requests.
 * This is achieved by classifying exceptions as fatal or as non fatal/retryable.
 * Evidently only subclasses can make the proper determination of what is retryable and what isn't.
 * This is useful in case the service employs throttling and to deal with general network issues.
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Antoine Snyers (antoine at atmire dot com)
 */
public abstract class AbstractRemoteMetadataSource {

    protected long lastRequest = 0;
    protected long interRequestTime;

    protected ReentrantLock lock = new ReentrantLock();

    protected int maxRetry = 20;
    protected int retry;
    protected String operationId;
    protected String warning;

    protected Map<Class, List<SourceExceptionHandler>> exceptionHandlersMap;
    protected Exception error;

    /**
     * Constructs an empty MetadataSource class object and initializes the Exceptionhandlers
     */
    protected AbstractRemoteMetadataSource() {
        initExceptionHandlers();
    }

    /**
     *  initialize the exceptionHandlersMap with an empty {@link java.util.LinkedHashMap}
     */
    protected void initExceptionHandlers() {
        exceptionHandlersMap = new LinkedHashMap<>();
        // if an exception is thrown that is not in there, it is not recoverable and the retry chain will stop
        // by default all exceptions are fatal, but subclasses can add their own handlers for their own exceptions
    }

    /**
     * Return the warning message used for logging during exception catching
     * @return a "warning" String
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Set the warning message used for logging
     * @param warning
     */
    public void setWarning(String warning) {
        this.warning = warning;
    }

    /**
     * Return the number of retries that have currently been undertaken
     * @return the number of retries
     */
    public int getRetry() {
        return retry;
    }
    /**
     * Return the number of max retries that can be undertaken before separate functionality kicks in
     * @return the number of maximum retries
     */
    public int getMaxRetry() {
        return maxRetry;
    }

    /**
     * Set the number of maximum retries before throwing on the exception
     * @param maxRetry
     */
    @Resource(name="maxRetry")
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    /**
     * Retrieve the operationId
     * @return A randomly generated UUID. generated during the retry method
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * Retrieve the last encountered exception
     * @return An Exception object, the last one encountered in the retry method
     */
    public Exception getError() {
        return error;
    }

    /**
     * Set the last encountered error
     * @param error
     */
    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AbstractRemoteMetadataSource.class);

    /**
     * Command pattern implementation. the callable.call method will be retried
     * until it either succeeds or reaches the try limit. Maybe this should have
     * a backoff algorithm instead of waiting a fixed time.
     *
     * @param callable the callable to call. See the classes with the same name as
     *                 the public methods of this class.
     * @param <T>      return type. Generics for type safety.
     * @return The result of the call
     * @throws org.dspace.importer.external.exception.MetadataSourceException if something unrecoverable happens (e.g. network failures)
     */
    protected <T> T retry(Callable<T> callable) throws MetadataSourceException {

        retry = 0;
        operationId = UUID.randomUUID().toString();
        while (true) {
            try {
                lock.lock();
                this.error = null;
                long time = System.currentTimeMillis() - lastRequest;
                if ((time) < interRequestTime) {
                    Thread.sleep(interRequestTime - time);
                }
                try {
                    init();
                } catch (Exception e) {
                    throwSourceException(retry, e, operationId);
                }
                log.info("operation " + operationId + " started");
                T response = callable.call();
                log.info("operation " + operationId + " successful");
                return response;
            } catch (Exception e) {
                this.error = e;
                if (retry > maxRetry) {
                    throwSourceException(retry, e, operationId);
                }
                handleException(retry, e, operationId);

                // No MetadataSourceException has interrupted the loop
                retry++;
                log.warn("Error in trying operation " + operationId + " " + retry + " " + warning + ", retrying !", e);

            } finally {
                lock.unlock();
            }

            try{
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throwSourceException(retry, e, operationId);
            }

        }

    }

    /**
     * Handles a given exception or throws on a {@link org.dspace.importer.external.exception.MetadataSourceException} if no ExceptionHandler is set
     * @param retry The number of retries before the exception was thrown on
     * @param exception The exception to handle
     * @param operationId The id of the operation that threw the exception
     * @throws MetadataSourceException if no ExceptionHandler is configured for the given exception
     */
    protected void handleException(int retry, Exception exception, String operationId) throws MetadataSourceException {

        List<SourceExceptionHandler> exceptionHandlers = getExceptionHandler(exception);
        if (exceptionHandlers != null && !exceptionHandlers.isEmpty()) {
            for (SourceExceptionHandler exceptionHandler : exceptionHandlers) {
                exceptionHandler.handle(this);
            }
        }else{
            throwSourceException(retry, exception, operationId);
        }
    }

    /** Retrieve a list of SourceExceptionHandler objects that have an instanceof the exception configured to them.
     * @param exception The exception to base the retrieval of {@link org.dspace.importer.external.exception.SourceExceptionHandler} on
     * @return a list of {@link org.dspace.importer.external.exception.SourceExceptionHandler} objects
     */
    protected List<SourceExceptionHandler> getExceptionHandler(Exception exception) {
        for (Class aClass : exceptionHandlersMap.keySet()) {
            if (aClass.isInstance(exception)) {
                return exceptionHandlersMap.get(aClass);
            }
        }
        return null;
    }

    /** Throw a {@link MetadataSourceException}
     * @param retry The number of retries before the exception was thrown on
     * @param exception The exception to throw
     * @param operationId The id of the operation that threw the exception
     * @throws MetadataSourceException
     */
    protected void throwSourceException(int retry, Exception exception, String operationId) throws MetadataSourceException {
        throwSourceExceptionHook();
        log.error("Source exception " + exception.getMessage(),exception);
        throw new MetadataSourceException("At retry of operation " + operationId + " " + retry, exception);
    }

    /**
     * A specified point where methods can be specified or callbacks can be executed
     */
    protected void throwSourceExceptionHook() {

    }

    /**
     * Attempts to init a session
     *
     * @throws Exception
     */
    public abstract void init() throws Exception;


}
