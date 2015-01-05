/*
 */
package org.dspace.workflow;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ApproveRejectReviewItemException extends Exception {
    public ApproveRejectReviewItemException() {
        super();
    }

    public ApproveRejectReviewItemException(String message) {
        super(message);
    }

    public ApproveRejectReviewItemException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ApproveRejectReviewItemException(Throwable throwable) {
        super(throwable);
    }
}
