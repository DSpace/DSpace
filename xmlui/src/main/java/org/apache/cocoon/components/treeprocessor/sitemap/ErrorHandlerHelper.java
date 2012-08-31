/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.treeprocessor.sitemap;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.Constants;
import org.apache.cocoon.Processor;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.notification.Notifying;
import org.apache.cocoon.components.notification.NotifyingBuilder;
import org.apache.cocoon.components.treeprocessor.InvokeContext;
import org.apache.cocoon.components.treeprocessor.ProcessingNode;
import org.apache.cocoon.core.container.spring.logger.LoggerUtils;
import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.util.AbstractLogEnabled;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Map;

/**
 * Helps to call error handlers from PipelineNode and PipelinesNode.
 *
 * @version $Id: ErrorHandlerHelper.java 607152 2007-12-28 01:32:07Z vgritsenko $
 */
public class ErrorHandlerHelper extends AbstractLogEnabled
                                implements Serviceable {

    private ServiceManager manager;

    /**
     * Logger for handled errors
     */
    protected Log handledErrorsLogger;

    /**
     * Error handling node for all other exceptions
     */
    private HandleErrorsNode error;


    /**
     * The component manager is used to create notifying builders.
     */
    public void service(ServiceManager manager) throws ServiceException {
        this.manager = manager;
        this.handledErrorsLogger = LoggerUtils.getChildLogger(this.manager, "handled");
    }

    void setErrorHandler(ProcessingNode node) {
        this.error = (HandleErrorsNode) node;
    }

    /**
     * @return true if has no error handler nodes set
     */
    public boolean isEmpty() {
        return this.error == null;
    }

    public boolean isInternal() {
        return this.error != null && this.error.isInternal();
    }

    public boolean isExternal() {
        return this.error != null && this.error.isExternal();
    }

    /**
     * Handle error.
     */
    public boolean invokeErrorHandler(Exception ex,
                                      Environment env,
                                      InvokeContext context)
    throws Exception {
        final Processor.InternalPipelineDescription desc = prepareErrorHandler(ex, env, context);
        if (desc != null) {
            context.setInternalPipelineDescription(desc);
            return true;
        }

        return false;
    }

    /**
     * Prepare error handler for the internal pipeline error handling.
     *
     * <p>If building pipeline only, error handling pipeline will be
     * built and returned. If building and executing pipeline,
     * error handling pipeline will be built and executed.</p>
     */
    public Processor.InternalPipelineDescription prepareErrorHandler(Exception ex,
                                                                     Environment env,
                                                                     InvokeContext context)
    throws Exception {
        boolean internal = !env.isExternal() && !env.isInternalRedirect();

        if (internal && !isInternal()) {
            // Propagate exception on internal request: No internal handler.
            throw ex;
        } else if (!internal && !isExternal()) {
            // Propagate exception on external request: No external handler.
            throw ex;
        } else if (error != null) {
            // Invoke error handler
            return prepareErrorHandler(error, ex, env, context);
        }

        // Exception was not handled by this error handler, propagate.
        throw ex;
    }

    /**
     * Prepare (or execute) error handler using specified error handler
     * processing node.
     *
     * <p>If building pipeline only, error handling pipeline will be
     * built and returned. If building and executing pipeline,
     * error handling pipeline will be built and executed.</p>
     */
    private Processor.InternalPipelineDescription prepareErrorHandler(ProcessingNode node,
                                                                      Exception ex,
                                                                      Environment env,
                                                                      InvokeContext context)
    throws Exception {
        Throwable rootException = ExceptionUtils.getRootCause(ex);
        if (rootException instanceof ResourceNotFoundException) {
            this.handledErrorsLogger.error(rootException.getMessage());
        } else {
            this.handledErrorsLogger.error(ex.getMessage(), ex);
        }

        try {
            prepare(context, env, ex);

            // Create error context
            InvokeContext errorContext = new InvokeContext(context, this.manager);
            try {
                // Process error handling node
                if (node.invoke(env, errorContext)) {
                    // Exception was handled.
                    return errorContext.getInternalPipelineDescription(env);
                }
            } finally {
                errorContext.dispose();
            }
        } catch (Exception e) {
            getLogger().error("An exception occured while handling errors at " + node.getLocation(), e);
            // Rethrow it: It will either be handled by the parent sitemap or by the environment (e.g. Cocoon servlet)
            throw e;
        }

        // Exception was not handled in this error handler, propagate.
        throw ex;
    }

//    private Throwable extractRootException(Exception exception) {
//        Throwable extracted = exception;
//        while(extracted.getCause() != null) {
//            extracted = extracted.getCause();
//        }
//        return extracted;
//    }


    /**
     * Build notifying object
     */
    private void prepare(InvokeContext context, Environment env, Exception ex)
    throws IOException, ServiceException {
        Map objectModel = env.getObjectModel();
        if (objectModel.get(ObjectModelHelper.THROWABLE_OBJECT) == null) {
            // error has not been processed by another handler before

            // Try to reset the response to avoid mixing already produced output
            // and error page.
            if (!context.isBuildingPipelineOnly()) {
                env.tryResetResponse();
            }

            // Create a Notifying (deprecated)
            NotifyingBuilder notifyingBuilder = (NotifyingBuilder) this.manager.lookup(NotifyingBuilder.ROLE);
            Notifying currentNotifying = null;
            try {
                currentNotifying = notifyingBuilder.build(this, ex);
            } finally {
                this.manager.release(notifyingBuilder);
            }
            objectModel.put(Constants.NOTIFYING_OBJECT, currentNotifying);

            // Add it to the object model
            objectModel.put(ObjectModelHelper.THROWABLE_OBJECT, ex);
        }
    }
}
