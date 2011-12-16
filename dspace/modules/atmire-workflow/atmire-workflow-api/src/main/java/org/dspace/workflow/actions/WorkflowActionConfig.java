package org.dspace.workflow.actions;

import org.dspace.core.ConfigurationManager;
import org.dspace.workflow.Step;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 3-aug-2010
 * Time: 16:22:56
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowActionConfig {

    protected Action processingAction;
    private String id;
    //TODO: interfaces for UIactions and processingactions
    private Step step;
    //TODO
//    private String name = this.toString();
    private boolean requiresUI;



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

    public boolean isRequiresUI() {
        return requiresUI;
    }

    //TODO: add jspui
    public boolean hasUserInterface() {
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

    public String getName() {
        //TODO: create another name ?
        return id;
    }
}
