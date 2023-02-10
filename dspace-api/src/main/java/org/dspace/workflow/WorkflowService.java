/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Service interface class for the WorkflowService framework.
 * All WorkflowServices service classes should implement this class since it offers some basic methods which all
 * Workflows
 * are required to have.
 *
 * @param <T> some implementation of workflow item.
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowService<T extends WorkflowItem> {


    /**
     * Move an Item from a submitter's workspace into a collection's workflow
     * - in a single transaction do away with
     * the WorkspaceItem and turn it into a WorkflowItem.
     * The WorkspaceItem which wraps the submitted Item is deleted.
     *
     * @param context The relevant DSpace Context.
     * @param wsi     The WorkspaceItem to convert to a workflow item
     * @return The resulting workflow item
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws WorkflowException  if workflow error
     */
    public T start(Context context, WorkspaceItem wsi)
        throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * Start the workflow normally, but disable notifications for the first
     * workflow step.  Subsequent notifications happen normally.  Useful for
     * large imports.
     *
     * @param c   The relevant DSpace Context.
     * @param wsi workspace item
     * @return the resulting workflow item.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws WorkflowException  if workflow error
     */
    public T startWithoutNotify(Context c, WorkspaceItem wsi)
        throws SQLException, AuthorizeException, IOException, WorkflowException;

    /**
     * abort() aborts a workflow, completely deleting it (administrator do this)
     * (it will basically do a reject from any state - the item ends up back in
     * the user's PersonalWorkspace
     *
     * @param c  The relevant DSpace Context.
     * @param wi WorkflowItem to operate on
     * @param e  EPerson doing the operation
     * @return workspace item returned to workspace
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public WorkspaceItem abort(Context c, T wi, EPerson e) throws SQLException, AuthorizeException, IOException;

    /**
     * Deletes workflow task item in correct order.
     *
     * @param c  The relevant DSpace Context.
     * @param wi The WorkflowItem that shall be deleted.
     * @param e  Admin that deletes this workflow task and item (for logging
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public void deleteWorkflowByWorkflowItem(Context c, T wi, EPerson e)
        throws SQLException, AuthorizeException, IOException;

    public WorkspaceItem sendWorkflowItemBackSubmission(Context c, T workflowItem, EPerson e, String provenance,
                                                        String rejection_message)
        throws SQLException, AuthorizeException, IOException;

    public void restartWorkflow(Context context, XmlWorkflowItem wi, EPerson decliner, String provenance)
        throws SQLException, AuthorizeException, IOException, WorkflowException;

    public String getMyDSpaceLink();

    public void deleteCollection(Context context, Collection collection)
        throws SQLException, IOException, AuthorizeException;

    public List<String> getEPersonDeleteConstraints(Context context, EPerson ePerson) throws SQLException;

    public Group getWorkflowRoleGroup(Context context, Collection collection, String roleName, Group roleGroup)
        throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException, WorkflowException;

    /**
     * This method will create the workflowRoleGroup for a collection and the given rolename
     * @param context       The relevant DSpace context
     * @param collection    The collection
     * @param roleName      The rolename
     * @return              The created Group
     * @throws AuthorizeException If something goes wrong
     * @throws SQLException If something goes wrong
     * @throws IOException If something goes wrong
     * @throws WorkflowConfigurationException If something goes wrong
     */
    public Group createWorkflowRoleGroup(Context context, Collection collection, String roleName)
        throws AuthorizeException, SQLException, IOException, WorkflowConfigurationException;

    public List<String> getFlywayMigrationLocations();
}
