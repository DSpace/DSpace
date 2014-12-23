/*
 */
package org.dspace.workflow;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
class ItemIsNotEligibleForStepException extends Exception {

    public ItemIsNotEligibleForStepException(String string) {
        super(string);
    }

}
