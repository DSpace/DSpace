/*
 */
package org.dspace.workflow;

import java.io.IOException;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
class ApproveBlackoutItemException extends Exception {

    public ApproveBlackoutItemException(String message) {
        super(message);
    }

    ApproveBlackoutItemException(String message, Throwable cause) {
        super(message, cause);
    }

}
