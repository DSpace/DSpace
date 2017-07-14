/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.WorkflowConfigurationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the WorkflowService framework.
 * All WorkflowServices service classes should implement this class since it offers some basic methods which all Workflows
 * are required to have.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowService<T extends WorkflowItem> {
    
    
    /**
     * startWorkflow() begins a workflow - in a single transaction do away with
     * the PersonalWorkspace entry and turn it into a WorkflowItem.
     *
     * @param context
     *            Context
     * @param wsi
     *            The WorkspaceItem to convert to a workflow item
     * @return The resulting workflow item
     */
    public T start(Context context, WorkspaceItem wsi) throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * startWithoutNotify() starts the workflow normally, but disables
     * notifications (useful for large imports,) for the first workflow step -
     * subsequent notifications happen normally
     */
    public T startWithoutNotify(Context c, WorkspaceItem wsi) throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * abort() aborts a workflow, completely deleting it (administrator do this)
     * (it will basically do a reject from any state - the item ends up back in
     * the user's PersonalWorkspace
     *
     * @param c
     *            Context
     * @param wi
     *            WorkflowItem to operate on
     * @param e
     *            EPerson doing the operation
     */
    public WorkspaceItem abort(Context c, T wi, EPerson e) throws SQLException, AuthorizeException, IOException;

    public WorkspaceItem sendWorkflowItemBackSubmission(Context c, T workflowItem, EPerson e, String provenance, String rejection_message) throws SQLException, AuthorizeException, IOException;

    public String getMyDSpaceLink();

    public void deleteCollection(Context context, Collection collection) throws SQLException, IOException, AuthorizeException;

    public List<String> getEPersonDeleteConstraints(Context context, EPerson ePerson) throws SQLException;

    public Group getWorkflowRoleGroup(Context context, Collection collection, String roleName, Group roleGroup) throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException, WorkflowException;

    public List<String> getFlywayMigrationLocations();
}
