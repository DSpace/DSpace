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
import org.dspace.content.Item;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Service interface class for the Workflow items.
 * All WorkflowItem service classes should implement this class since it offers some basic methods which all
 * WorkflowItems
 * are required to have.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkflowItemService extends InProgressSubmissionService<WorkflowItem> {

    public WorkflowItem create(Context context, Item item, Collection collection) throws SQLException,
            AuthorizeException;

    /**
     * Get a workflow item from the database.
     *
     * @param context The relevant DSpace Context.
     * @param id      ID of the workflow item
     * @return the workflow item, or null if the ID is invalid.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public WorkflowItem find(Context context, int id) throws SQLException;

    /**
     * return all workflowitems
     *
     * @param context The relevant DSpace Context.
     * @return List of all workflowItems in system
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<WorkflowItem> findAll(Context context) throws SQLException;

    /**
     * Get all workflow items for a particular collection.
     *
     * @param context    The relevant DSpace Context.
     * @param collection the collection
     * @return array of the corresponding workflow items
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<WorkflowItem> findByCollection(Context context, Collection collection) throws SQLException;

    /**
     * Check to see if a particular item is currently under Workflow.
     * If so, its WorkflowItem is returned.  If not, null is returned
     *
     * @param context The relevant DSpace Context.
     * @param item    the item
     * @return workflow item corresponding to the item, or null
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public WorkflowItem findByItem(Context context, Item item) throws SQLException;

    /**
     * Get all workflow items that were original submissions by a particular
     * e-person.
     *
     * @param context The relevant DSpace Context.
     * @param ep      the eperson
     * @return the corresponding workflow items
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException;

    /**
     * Delete all workflow items present in the specified collection.
     *
     * @param context    The relevant DSpace Context.
     * @param collection the containing collection
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *         to perform a particular action.
     */
    public void deleteByCollection(Context context, Collection collection)
            throws SQLException, IOException, AuthorizeException;

    /**
     * Delete the specified workflow item.
     *
     * @param context      The relevant DSpace Context.
     * @param workflowItem which workflow item to delete
     * @throws IOException        A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public void delete(Context context, WorkflowItem workflowItem)
            throws SQLException, AuthorizeException, IOException;

    /**
     * return all workflowitems for a certain page
     *
     * @param context  The relevant DSpace Context.
     * @param page     paging: page number
     * @param pagesize paging: items per page
     * @return WorkflowItem list of all the workflow items in system
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<WorkflowItem> findAll(Context context, Integer page, Integer pagesize) throws SQLException;

    /**
     * return all workflowitems for a certain page with a certain collection
     *
     * @param context    The relevant DSpace Context.
     * @param page       paging: page number
     * @param pagesize   paging: items per page
     * @param collection restrict to this collection
     * @return WorkflowItem list of all the workflow items in system
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<WorkflowItem> findAllInCollection(Context context, Integer page, Integer pagesize,
                                                     Collection collection) throws SQLException;

    /**
     * return how many workflow items appear in the database
     *
     * @param context The relevant DSpace Context.
     * @return the number of workflow items
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public int countAll(Context context) throws SQLException;

    /**
     * return how many workflow items that appear in the collection
     *
     * @param context    The relevant DSpace Context.
     * @param collection restrict to this collection
     * @return the number of workflow items
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public int countAllInCollection(Context context, Collection collection) throws SQLException;

    /**
     * Return all the workflow items from a specific submitter respecting the pagination parameters
     *
     * @param context
     *            The relevant DSpace Context.
     * @param ep
     *            the eperson that has submitted the item
     * @param pageNumber
     *            paging: page number
     * @param pageSize
     *            paging: items per page
     * @return
     * @throws SQLException
     */
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep, Integer pageNumber, Integer pageSize)
            throws SQLException;

    /**
     * Count the number of workflow items from a specific submitter
     *
     * @param context
     *            The relevant DSpace Context.
     * @param ep
     *            the eperson that has submitted the item
     * @return
     * @throws SQLException
     */
    public int countBySubmitter(Context context, EPerson ep) throws SQLException;

}
