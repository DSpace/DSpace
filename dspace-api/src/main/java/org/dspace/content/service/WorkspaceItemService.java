/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service interface class for the WorkspaceItem object.
 * The implementation of this class is responsible for all business logic calls for the WorkspaceItem object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface WorkspaceItemService extends InProgressSubmissionService<WorkspaceItem>{

    /**
     * Get a workspace item from the database. The item, collection and
     * submitter are loaded into memory.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the workspace item
     *
     * @return the workspace item, or null if the ID is invalid.
     * @throws SQLException if database error
     */
    public WorkspaceItem find(Context context, int id) throws SQLException;


    /**
     * Create a new workspace item, with a new ID. An Item is also created. The
     * submitter is the current user in the context.
     *
     * @param context
     *            DSpace context object
     * @param collection
     *            Collection being submitted to
     * @param template
     *            if <code>true</code>, the workspace item starts as a copy
     *            of the collection's template item
     *
     * @return the newly created workspace item
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public WorkspaceItem create(Context context, Collection collection,  boolean template)
            throws AuthorizeException, SQLException;

    public WorkspaceItem create(Context c, WorkflowItem wfi) throws SQLException, AuthorizeException;


    /**
     * Get all workspace items for a particular e-person. These are ordered by
     * workspace item ID, since this should likely keep them in the order in
     * which they were created.
     *
     * @param context
     *            the context object
     * @param ep
     *            the eperson
     *
     * @return the corresponding workspace items
     * @throws SQLException if database error
     */
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep)
            throws SQLException;

    /**
     * Get all workspace items for a particular collection.
     *
     * @param context
     *            the context object
     * @param collection
     *            the collection
     *
     * @return the corresponding workspace items
     * @throws SQLException if database error
     */
    public List<WorkspaceItem> findByCollection(Context context, Collection collection)
            throws SQLException;


    /**
     * Check to see if a particular item is currently still in a user's Workspace.
     * If so, its WorkspaceItem is returned.  If not, null is returned
     *
     * @param context
     *            the context object
     * @param item
     *            the item
     *
     * @return workflow item corresponding to the item, or null
     * @throws SQLException if database error
     */
    public WorkspaceItem findByItem(Context context, Item item)
            throws SQLException;

    public List<WorkspaceItem> findAllSupervisedItems(Context context) throws SQLException;

    public List<WorkspaceItem> findSupervisedItemsByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Get all workspace items in the whole system
     *
     * @param   context     the context object
     *
     * @return      all workspace items
     * @throws SQLException if database error
     */
    public List<WorkspaceItem> findAll(Context context)
        throws SQLException;

    /**
     * Delete the workspace item. The entry in workspaceitem, the unarchived
     * item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     * @param context context
     * @param workspaceItem workspace item
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public void deleteAll(Context context, WorkspaceItem workspaceItem) throws SQLException, AuthorizeException, IOException;

    int countTotal(Context context) throws SQLException;

    /**
     * The map entry returned contains stage reached as the key and count of items in that stage as a value
     * @param context
     * @return the map
     * @throws SQLException if database error
     */
    List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException;
}
