/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

import java.util.List;

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

    public WorkflowActionConfig(String id) {
        this.id = id;
    }

    public void setProcessingAction(Action processingAction) {
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

    /**
     * Returns a list of options the user has on this action, resulting in the next step of the workflow
     * @return  A list of options of this action, resulting in the next step of the workflow
     */
    public List<String> getOptions() {
        return this.processingAction.getOptions();
    }

    /**
     * Returns a list of advanced options this user has on this action, resulting in the next step of the workflow
     * @return A list of advanced options of this action, resulting in the next step of the workflow
     */
    public List<String> getAdvancedOptions() {
        return this.processingAction.getAdvancedOptions();
    }

    /**
     * Returns a boolean depending on whether this action has advanced options
     * @return The boolean indicating whether this action has advanced options
     */
    public boolean isAdvanced() {
        return this.processingAction.isAdvanced();
    }

    /**
     * Returns a Map of info for the advanced options this user has on this action
     * @return a Map of info for the advanced options this user has on this action
     */
    public List<ActionAdvancedInfo> getAdvancedInfo() {
        return this.processingAction.getAdvancedInfo();
    }

}
