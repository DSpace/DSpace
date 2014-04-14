/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

import org.dspace.xmlworkflow.state.Step;

/**
 * Configuration class for an action
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowActionConfig {

    protected Action processingAction;
    private String id;
    private Step step;
    private boolean requiresUI;


    private ActionInterface actionUI;



    public WorkflowActionConfig(String id){
        this.id = id;
    }

    public void setProcessingAction(Action processingAction){
        this.processingAction = processingAction;
        processingAction.setParent(this);

    }

    public Action getProcessingAction() {
        return processingAction;
    }

    public void setRequiresUI(boolean requiresUI) {
        this.requiresUI = requiresUI;
    }

    public boolean requiresUI() {
        return requiresUI;
    }

    public String getId() {
        return id;
    }



    public void setStep(Step step) {
        this.step = step;
    }

    public Step getStep() {
        return step;
    }

    public ActionInterface getActionUI() {
        return actionUI;
    }

    public void setActionUI(ActionInterface actionUI) {
        this.actionUI = actionUI;
    }
}
