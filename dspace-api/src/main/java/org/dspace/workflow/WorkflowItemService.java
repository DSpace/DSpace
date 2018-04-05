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
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the Workflow items.
 * All WorkflowItem service classes should implement this class since it offers some basic methods which all WorkflowItems
 * are required to have.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowItemService<T extends WorkflowItem> extends InProgressSubmissionService<T> {

    public T create(Context context, Item item, Collection collection) throws SQLException, AuthorizeException;

    /**
     * Get a workflow item from the database.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the workflow item
     *
     * @return the workflow item, or null if the ID is invalid.
     */
    public T find(Context context, int id) throws SQLException;

    /**
     * return all workflowitems
     *
     * @param context  active context
     * @return List of all workflowItems in system
     */
    public List<T> findAll(Context context) throws SQLException;

    /**
     * Get all workflow items for a particular collection.
     *
     * @param context
     *            the context object
     * @param collection
     *            the collection
     *
     * @return array of the corresponding workflow items
     */
    public List<T> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Check to see if a particular item is currently under Workflow.
     * If so, its WorkflowItem is returned.  If not, null is returned
     *
     * @param context
     *            the context object
     * @param item
     *            the item
     *
     * @return workflow item corresponding to the item, or null
     */
    public T findByItem(Context context, Item item) throws SQLException;

    /**
     * Get all workflow items that were original submissions by a particular
     * e-person.
     *
     * @param context
     *            the context object
     * @param ep
     *            the eperson
     *
     * @return the corresponding workflow items
     */
    public List<T> findBySubmitter(Context context, EPerson ep) throws SQLException;

    public void deleteByCollection(Context context, Collection collection) throws SQLException, IOException, AuthorizeException;


    public void delete(Context context, T workflowItem) throws SQLException, AuthorizeException, IOException;

}
