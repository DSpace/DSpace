/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * A class that contains utility methods related to the workflow.
 * The adding/removing of claimed users, and ensuring that
 * if multiple users have to perform these steps that a count is kept so that
 * no more than the allowed user count are allowed to perform their actions.
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface WorkflowRequirementsService {

    public static final String WORKFLOW_SCHEMA = "workflow";


    /**
     * Adds a claimed user in the metadata.
     * If enough users have claimed this task (claimed or finished) to meet the
     * required number, the pooled tasks will be deleted.
     *
     * @param context the dspace context
     * @param wfi     the workflow item
     * @param step    the step for which we are accepting
     * @param user    the current user
     * @throws SQLException       ...
     * @throws AuthorizeException ...
     * @throws IOException        ...
     */
    public void addClaimedUser(Context context, XmlWorkflowItem wfi, Step step, EPerson user)
        throws SQLException, AuthorizeException, IOException;

    public void removeClaimedUser(Context context, XmlWorkflowItem wfi, EPerson user, String stepID)
        throws SQLException, IOException, WorkflowConfigurationException, AuthorizeException;

    /**
     * Adds a finished user in the metadata.
     * This method will also remove the user from the in-progress metadata.
     *
     * @param context the dspace context
     * @param wfi     the workflow item
     * @param user    the current user
     * @throws AuthorizeException ...
     * @throws SQLException       ...
     */
    public void addFinishedUser(Context context, XmlWorkflowItem wfi, EPerson user)
        throws AuthorizeException, SQLException;

    public void clearInProgressUsers(Context context, XmlWorkflowItem wfi)
            throws AuthorizeException, SQLException;
}
