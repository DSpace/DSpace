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
     *     The relevant DSpace Context.
     * @param id
     *     ID of the workflow item
     *
     * @return the workflow item, or null if the ID is invalid.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public T find(Context context, int id) throws SQLException;

    /**
     * return all workflowitems
     *
     * @param context
     *     The relevant DSpace Context.
     *
     * @return List of all workflowItems in system
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<T> findAll(Context context) throws SQLException;

    /**
     * Get all workflow items for a particular collection.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param collection
     *     the collection
     *
     * @return array of the corresponding workflow items
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<T> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Check to see if a particular item is currently under Workflow.
     * If so, its WorkflowItem is returned.  If not, null is returned
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     the item
     *
     * @return workflow item corresponding to the item, or null
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public T findByItem(Context context, Item item) throws SQLException;

    /**
     * Get all workflow items that were original submissions by a particular
     * e-person.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param ep
     *     the eperson
     *
     * @return the corresponding workflow items
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<T> findBySubmitter(Context context, EPerson ep) throws SQLException;

    /**
     * Delete all workflow items present in the specified collection.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param collection
     *     the containing collection
     *
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void deleteByCollection(Context context, Collection collection) throws SQLException, IOException, AuthorizeException;

    /**
     * Delete the specified workflow item.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param workflowItem
     *     which workflow item to delete
     *
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void delete(Context context, T workflowItem) throws SQLException, AuthorizeException, IOException;

}
