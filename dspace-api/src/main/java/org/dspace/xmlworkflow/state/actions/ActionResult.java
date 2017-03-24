/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

/**
 * Represent the result of an {@link Action}.
 * The result consists of 2 parts: a type and and a result.
 *
 * <p>
 * The type is represented by an Enum and can either be something like:
 * <dl>
 *   <dt>TYPE_OUTCOME:</dt>         <dd>we have a certain outcome so move to another action/step</dd>
 *   <dt>TYPE_ERROR:</dt>           <dd>an error has occurred while processing the action</dd>
 *   <dt>TYPE_PAGE:</dt>
 *   <dt>TYPE_CANCEL:</dt>
 *   <dt>TYPE_SUBMISSION_PAGE:</dt>
 * </dl>
 *
 * <p>
 * The optional result integer is used to determine
 * the next step once the action has completed.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ActionResult {

    public static enum TYPE{
        TYPE_OUTCOME,
        TYPE_PAGE,
        TYPE_ERROR,
        TYPE_CANCEL,
        TYPE_SUBMISSION_PAGE
    }

    public static final int OUTCOME_COMPLETE = 0;

    /** The type is used to send the user to the submission page, to another page in the step, to move to another step, ... */
    private TYPE type;
    /** The result will determine what our next step is. */
    private int result;

    public ActionResult(TYPE type, int result) {
        this.type = type;
        this.result = result;
    }

    public ActionResult(TYPE type) {
        this.type = type;
        this.result = -1;
    }

    public int getResult() {
        return result;
    }

    public TYPE getType() {
        return type;
    }
}


