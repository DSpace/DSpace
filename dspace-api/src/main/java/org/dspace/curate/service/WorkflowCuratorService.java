/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.workflowbasic.BasicWorkflowItem;

import java.io.IOException;
import java.sql.SQLException;

/**
 * WorkflowCurator manages interactions between curation and workflow.
 * Specifically, it is invoked in WorkflowManager to allow the
 * performance of curation tasks during workflow.
 *
 * @author richardrodgers
 */
public interface WorkflowCuratorService {


    public boolean needsCuration(BasicWorkflowItem wfi);

    /**
     * Determines and executes curation on a Workflow item.
     *
     * @param c the context
     * @param wfi the workflow item
     * @return true if curation was completed or not required,
     *         false if tasks were queued for later completion,
     *         or item was rejected
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean doCuration(Context c, BasicWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException;


    /**
     * Determines and executes curation of a Workflow item.
     *
     * @param curator the Curator object
     * @param c the user context
     * @param wfId the workflow id
     * @return true if curation was completed or not required,
     *         false if no workflow item found for id
     *         or item was rejected
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean curate(Curator curator, Context c, String wfId)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Determines and executes curation of a Workflow item.
     *
     * @param curator the Curator object
     * @param c the user context
     * @param wfi the workflow item
     * @return true if curation was completed or not required,
     *         false if item was rejected
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean curate(Curator curator, Context c, BasicWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException;
}
