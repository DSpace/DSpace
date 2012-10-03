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
}
