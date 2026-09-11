/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * Extends the standard usage event to contain workflow information
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class UsageWorkflowEvent extends UsageEvent {

    private String workflowStep;
    private String oldState;
    private String currentWorkflowAction;
    private String previousWorkflowAction;
    private boolean previousActionRequiresUI;
    private EPerson[] epersonOwners;
    private Group[] groupOwners;
    private Collection scope;
    private EPerson actor;
    private InProgressSubmission workflowItem;

    public UsageWorkflowEvent(Context context, Item item, InProgressSubmission workflowItem, String workflowStep,
                              String oldState, Collection scope, EPerson actor) {
        super(Action.WORKFLOW, null, context, item);
        this.workflowItem = workflowItem;
        this.workflowStep = workflowStep;
        this.oldState = oldState;
        this.scope = scope;
        this.actor = actor;
    }

    public String getWorkflowStep() {
        return workflowStep;
    }


    public String getOldState() {
        return oldState;
    }

    public String getCurrentWorkflowAction() {
        return currentWorkflowAction;
    }

    public void setCurrentWorkflowAction(String currentWorkflowAction) {
        this.currentWorkflowAction = currentWorkflowAction;
    }

    public String getPreviousWorkflowAction() {
        return previousWorkflowAction;
    }

    public void setPreviousWorkflowAction(String previousWorkflowAction) {
        this.previousWorkflowAction = previousWorkflowAction;
    }

    public boolean isPreviousActionRequiresUI() {
        return previousActionRequiresUI;
    }

    public void setPreviousActionRequiresUI(boolean previousActionRequiresUI) {
        this.previousActionRequiresUI = previousActionRequiresUI;
    }

    public Collection getScope() {
        return scope;
    }

    public EPerson[] getEpersonOwners() {
        return epersonOwners;
    }

    public void setEpersonOwners(EPerson... epersonOwners) {
        this.epersonOwners = epersonOwners;
    }

    public Group[] getGroupOwners() {
        return groupOwners;
    }

    public void setGroupOwners(Group... newGroupOwner) {
        this.groupOwners = newGroupOwner;
    }

    public EPerson getActor() {
        return actor;
    }

    public InProgressSubmission getWorkflowItem() {
        return workflowItem;
    }
}
