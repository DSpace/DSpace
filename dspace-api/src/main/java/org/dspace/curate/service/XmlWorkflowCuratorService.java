/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Manage interactions between curation and workflow.
 * Specifically, it is invoked in XmlWorkflowService to allow the
 * performance of curation tasks during workflow.
 *
 * Copied from {@link WorkflowCuratorService} with minor refactoring.
 *
 * @author mwood
 */
public interface XmlWorkflowCuratorService {
    /**
     * Should this item be curated?
     *
     * @param c current DSpace session.
     * @param wfi the item in question.
     * @return true if the item is in a workflow step.
     * @throws IOException passed through.
     * @throws SQLException passed through.
     */
    public boolean needsCuration(Context c, XmlWorkflowItem wfi)
            throws IOException, SQLException;

    /**
     * Determines and executes curation on a Workflow item.
     *
     * @param c the context
     * @param wfi the workflow item
     * @return true if curation was completed or not required;
     *         false if tasks were queued for later completion,
     *         or item was rejected.
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean doCuration(Context c, XmlWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Determines and executes curation of a Workflow item by ID.
     *
     * @param curator the curation context
     * @param c the user context
     * @param wfId the workflow item's ID
     * @return true if curation curation was completed or not required;
     *         false if tasks were queued for later completion,
     *         or item was rejected.
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean curate(Curator curator, Context c, String wfId)
            throws AuthorizeException, IOException, SQLException;

    /**
     * Determines and executes curation of a Workflow item.
     *
     * @param curator the curation context
     * @param c the user context
     * @param wfi the workflow item
     * @return true if workflow curation was completed or not required;
     *         false if tasks were queued for later completion,
     *         or item was rejected.
     * @throws AuthorizeException if authorization error
     * @throws IOException if IO error
     * @throws SQLException if database error
     */
    public boolean curate(Curator curator, Context c, XmlWorkflowItem wfi)
            throws AuthorizeException, IOException, SQLException;
}
