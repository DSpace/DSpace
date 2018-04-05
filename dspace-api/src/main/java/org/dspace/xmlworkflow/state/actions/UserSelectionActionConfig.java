/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

import org.dspace.xmlworkflow.state.actions.userassignment.UserSelectionAction;

/**
 * A class containing the user selection action configuration
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class UserSelectionActionConfig extends WorkflowActionConfig{

    public UserSelectionActionConfig(String id) {
        super(id);
    }

    public void setProcessingAction(UserSelectionAction processingAction){
        this.processingAction = processingAction;
        processingAction.setParent(this);

    }


    @Override
    public UserSelectionAction getProcessingAction(){
        return (UserSelectionAction) processingAction;
    }
}
