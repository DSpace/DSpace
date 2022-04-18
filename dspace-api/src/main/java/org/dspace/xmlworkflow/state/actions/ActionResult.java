/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

/**
 * Represents the result of an {@link Action}.
 * The result consists of 2 parts:  a type and a result.
 *
 * <p>
 * The type is represented by {@link TYPE an Enum} and can be something like:
 * <dl>
 *   <dt>TYPE_OUTCOME:</dt>         <dd>we have a certain outcome so move to another action/step.</dd>
 *   <dt>TYPE_ERROR:</dt>           <dd>an error has occurred while processing the action.</dd>
 *   <dt>TYPE_PAGE:</dt>            <dd>return to a specified page</dd>
 *   <dt>TYPE_CANCEL:</dt>          <dd>cancel the action</dd>
 *   <dt>TYPE_SUBMISSION_PAGE:</dt> <dd>return to the submission page</dd>
 * </dl>
 *
 * <p>
 * The optional result integer is used to determine
 * the next step once the action has completed.  If not set, it will be -1.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ActionResult {

    /** The decision of the Action's user(s) */
    public static enum TYPE {
        /** The action is satisfied.  See the result for details. */
        TYPE_OUTCOME,
        /** Return to a page specified in the result. */
        TYPE_PAGE,
        /** An error occurred in processing the action. */
        TYPE_ERROR,
        /** Cancel this action. */
        TYPE_CANCEL,
        /** Return to the submission page. */
        TYPE_SUBMISSION_PAGE
    }

    /** Outcome code which indicates that the Action was completed. */
    public static final int OUTCOME_COMPLETE = 0;

    /**
     * The type is used to send the user to the submission page, to another page
     * in the step, to move to another step, ...
     */
    private final TYPE type;

    /**
     * The result int will determine what our next step is.
     */
    private final int result;

    /**
     * Action taken, with a detailed result.
     * @param type how the workflow should proceed.
     * @param result detail for how to proceed.
     */
    public ActionResult(TYPE type, int result) {
        this.type = type;
        this.result = result;
    }

    /**
     * Action taken without a result.
     * @param type how the workflow should proceed.
     */
    public ActionResult(TYPE type) {
        this.type = type;
        this.result = -1;
    }

    /** @return details of the users' decision. */
    public int getResult() {
        return result;
    }

    /** @return the decision of the Action's users. */
    public TYPE getType() {
        return type;
    }
}


