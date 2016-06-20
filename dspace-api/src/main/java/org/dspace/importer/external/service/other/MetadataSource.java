/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.other;

import org.apache.log4j.Logger;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.SourceExceptionHandler;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 27 Oct 2014
 */
public abstract class MetadataSource {

    protected long lastRequest = 0;
    protected long interRequestTime;

    protected ReentrantLock lock = new ReentrantLock();

    protected int maxRetry = 20;
    protected int retry;
    protected String operationId;
    protected String warning;

    protected Map<Class, List<SourceExceptionHandler>> exceptionHandlersMap;
    protected Exception error;


    protected MetadataSource() {
        initExceptionHandlers();
    }

    protected void initExceptionHandlers() {
        exceptionHandlersMap = new LinkedHashMap<Class, List<SourceExceptionHandler>>();
        // if an exception is thrown that is not in there, it is not recoverable and the retry chain will stop
        // by default all exceptions are fatal, but subclasses can add their own handlers for their own exceptions
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public int getRetry() {
        return retry;
    }

    public int getMaxRetry() {
        return maxRetry;
    }
    @Resource(name="maxRetry")
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public String getOperationId() {
        return operationId;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(MetadataSource.class);

    /**
     * Command pattern implementation. the callable.call method will be retried
     * until it either succeeds or reaches the try limit. Maybe this should have
     * a backoff algorithm instead of waiting a fixed time.
     *
     * @param callable the callable to call. See the classes with the same name as
     *                 the public methods of this class.
     * @param <T>      return type. Generics for type safety.
     * @return The result of the call
     * @throws com.atmire.import_citations.configuration.SourceException if something unrecoverable happens (e.g. network failures)
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

    protected void handleException(int retry, Exception e, String operationId) throws MetadataSourceException {

        List<SourceExceptionHandler> exceptionHandlers = getExceptionHandler(e);
        if (exceptionHandlers != null && !exceptionHandlers.isEmpty()) {
            for (SourceExceptionHandler exceptionHandler : exceptionHandlers) {
                exceptionHandler.handle(this);
            }
        }else{
            throwSourceException(retry, e, operationId);
        }
    }

    protected List<SourceExceptionHandler> getExceptionHandler(Exception e) {
        for (Class aClass : exceptionHandlersMap.keySet()) {
            if (aClass.isInstance(e)) {
                return exceptionHandlersMap.get(aClass);
            }
        }
        return null;
    }

    protected void throwSourceException(int retry, Exception e, String operationId) throws MetadataSourceException {
        throwSourceExceptionHook();
//        log.error("Source exception", e);
        log.error("Source exception " + e.getMessage());
        throw new MetadataSourceException("At retry of operation " + operationId + " " + retry, e);
    }

    protected void throwSourceExceptionHook() {

    }

    /**
     * Attempts to init a session
     *
     * @throws Exception if error
     */
    public abstract void init() throws Exception;


}
