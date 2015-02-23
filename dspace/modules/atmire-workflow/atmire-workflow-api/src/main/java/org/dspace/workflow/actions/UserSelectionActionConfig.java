package org.dspace.workflow.actions;

import org.dspace.workflow.actions.userassignment.UserSelectionAction;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 6-aug-2010
 * Time: 14:57:17
 * To change this template use File | Settings | File Templates.
 */
public class UserSelectionActionConfig extends WorkflowActionConfig{

    public UserSelectionActionConfig(String id) {
        super(id);
    }

    public UserSelectionAction getProcessingAction(){
        return (UserSelectionAction) processingAction;
    }

    // Spring requires getter/setter types to match, so even though
    // we don't need a custom setter, we must have a setter that takes the same
    // parameter type as the above getter.
    public void setProcessingAction(UserSelectionAction processingAction) {
        super.setProcessingAction(processingAction);
    }
}
