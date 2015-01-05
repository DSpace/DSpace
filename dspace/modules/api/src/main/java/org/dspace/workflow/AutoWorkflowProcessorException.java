/*
 */
package org.dspace.workflow;

import java.io.IOException;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
class AutoWorkflowProcessorException extends Exception {

    public AutoWorkflowProcessorException(String message) {
        super(message);
    }

    AutoWorkflowProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

}
