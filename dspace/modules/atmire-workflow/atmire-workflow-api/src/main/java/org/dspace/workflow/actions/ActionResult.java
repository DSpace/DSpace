package org.dspace.workflow.actions;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 11-aug-2010
 * Time: 13:55:38
 */
public class ActionResult {

    //TODO: make enum configurable
    public static enum TYPE{
        TYPE_OUTCOME,
        TYPE_PAGE,
        TYPE_ERROR,
        TYPE_CANCEL,
        TYPE_SUBMISSION_PAGE
    }

    public static final int OUTCOME_COMPLETE = 0;

    private TYPE type;
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


