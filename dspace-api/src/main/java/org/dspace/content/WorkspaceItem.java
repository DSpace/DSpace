/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing an item in the process of being submitted by a user
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class WorkspaceItem implements InProgressSubmission
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    /** The item this workspace object pertains to */
    private Item item;

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this workspace item */
    private TableRow wiRow;

    /** The collection the item is being submitted to */
    private Collection collection;

    /**
     * Construct a workspace item corresponding to the given database row
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the database row
     */
    WorkspaceItem(Context context, TableRow row) throws SQLException
    {
        ourContext = context;
        wiRow = row;

        item = Item.find(context, wiRow.getIntColumn("item_id"));
        collection = Collection.find(context, wiRow
                .getIntColumn("collection_id"));

        // Cache ourselves
        context.cache(this, row.getIntColumn("workspace_item_id"));
    }

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
     */
    public static WorkspaceItem find(Context context, int id)
            throws SQLException
    {
        // First check the cache
        WorkspaceItem fromCache = (WorkspaceItem) context.fromCache(
                WorkspaceItem.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "workspaceitem", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "not_found,workspace_item_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workspace_item",
                        "workspace_item_id=" + id));
            }

            return new WorkspaceItem(context, row);
        }
    }

    /**
     * Create a new workspace item, with a new ID. An Item is also created. The
     * submitter is the current user in the context.
     * 
     * @param c
     *            DSpace context object
     * @param coll
     *            Collection being submitted to
     * @param template
     *            if <code>true</code>, the workspace item starts as a copy
     *            of the collection's template item
     * 
     * @return the newly created workspace item
     */
    public static WorkspaceItem create(Context c, Collection coll,
            boolean template) throws AuthorizeException, SQLException,
            IOException
    {
        // Check the user has permission to ADD to the collection
        AuthorizeManager.authorizeAction(c, coll, Constants.ADD);

        // Create an item
        Item i = Item.create(c);
        i.setSubmitter(c.getCurrentUser());

        // Now create the policies for the submitter and workflow
        // users to modify item and contents
        // contents = bitstreams, bundles
        // FIXME: icky hardcoded workflow steps
        Group step1group = coll.getWorkflowGroup(1);
        Group step2group = coll.getWorkflowGroup(2);
        Group step3group = coll.getWorkflowGroup(3);

        EPerson e = c.getCurrentUser();

        // read permission
        AuthorizeManager.addPolicy(c, i, Constants.READ, e, ResourcePolicy.TYPE_SUBMISSION);


        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("originalworkflow")) {
            if (step1group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.READ, step1group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step2group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.READ, step2group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step3group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.READ, step3group, ResourcePolicy.TYPE_WORKFLOW);
            }
        }


        // write permission
        AuthorizeManager.addPolicy(c, i, Constants.WRITE, e, ResourcePolicy.TYPE_SUBMISSION);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("originalworkflow")) {
            if (step1group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.WRITE, step1group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step2group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.WRITE, step2group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step3group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.WRITE, step3group, ResourcePolicy.TYPE_WORKFLOW);
            }
        }

        // add permission
        AuthorizeManager.addPolicy(c, i, Constants.ADD, e, ResourcePolicy.TYPE_SUBMISSION);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("originalworkflow")) {
            if (step1group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.ADD, step1group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step2group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.ADD, step2group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step3group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.ADD, step3group, ResourcePolicy.TYPE_WORKFLOW);
            }
        }

        // remove contents permission
        AuthorizeManager.addPolicy(c, i, Constants.REMOVE, e, ResourcePolicy.TYPE_SUBMISSION);

        if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("originalworkflow")) {
            if (step1group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step1group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step2group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step2group, ResourcePolicy.TYPE_WORKFLOW);
            }

            if (step3group != null)
            {
                AuthorizeManager.addPolicy(c, i, Constants.REMOVE, step3group, ResourcePolicy.TYPE_WORKFLOW);
            }
        }

        // Copy template if appropriate
        Item templateItem = coll.getTemplateItem();

        if (template && (templateItem != null))
        {
            Metadatum[] md = templateItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (int n = 0; n < md.length; n++)
            {
                i.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
                        md[n].value);
            }
        }

        i.update();

        // Create the workspace item row
        TableRow row = DatabaseManager.row("workspaceitem");

        row.setColumn("item_id", i.getID());
        row.setColumn("collection_id", coll.getID());

        log.info(LogManager.getHeader(c, "create_workspace_item",
                "workspace_item_id=" + row.getIntColumn("workspace_item_id")
                        + "item_id=" + i.getID() + "collection_id="
                        + coll.getID()));

        DatabaseManager.insert(c, row);

        WorkspaceItem wi = new WorkspaceItem(c, row);

        return wi;
    }

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
     */
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
            throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context, "workspaceitem",
                "SELECT workspaceitem.* FROM workspaceitem, item WHERE " +
                "workspaceitem.item_id=item.item_id AND " +
                "item.submitter_id= ? " +
                "ORDER BY workspaceitem.workspace_item_id", 
                ep.getID());

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                WorkspaceItem wi = (WorkspaceItem) context.fromCache(
                        WorkspaceItem.class, row.getIntColumn("workspace_item_id"));

                if (wi == null)
                {
                    wi = new WorkspaceItem(context, row);
                }

                wsItems.add(wi);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        return wsItems.toArray(new WorkspaceItem[wsItems.size()]);
    }

    /**
     * Get all workspace items for a particular collection.
     * 
     * @param context
     *            the context object
     * @param c
     *            the collection
     * 
     * @return the corresponding workspace items
     */
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
            throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();

        TableRowIterator tri = DatabaseManager.queryTable(context, "workspaceitem",
                "SELECT workspaceitem.* FROM workspaceitem WHERE " +
                "workspaceitem.collection_id= ? ",
                c.getID());

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                WorkspaceItem wi = (WorkspaceItem) context.fromCache(
                        WorkspaceItem.class, row.getIntColumn("workspace_item_id"));

                // not in cache? turn row into workspaceitem
                if (wi == null)
                {
                    wi = new WorkspaceItem(context, row);
                }

                wsItems.add(wi);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        return wsItems.toArray(new WorkspaceItem[wsItems.size()]);
    }

    /**
     * Check to see if a particular item is currently still in a user's Workspace.
     * If so, its WorkspaceItem is returned.  If not, null is returned
     *
     * @param context
     *            the context object
     * @param i
     *            the item
     *
     * @return workflow item corresponding to the item, or null
     */
    public static WorkspaceItem findByItem(Context context, Item i)
            throws SQLException
    {
        // Look for the unique workspaceitem entry where 'item_id' references this item
        TableRow row =  DatabaseManager.findByUnique(context, "workspaceitem", "item_id", i.getID());

        if (row == null)
        {
            return null;
        }
        else
        {
            return new WorkspaceItem(context, row);
        }
    }


    /**
     * Get all workspace items in the whole system
     *
     * @param   context     the context object
     *
     * @return      all workspace items
     */
    public static WorkspaceItem[] findAll(Context context)
        throws SQLException
    {
        List<WorkspaceItem> wsItems = new ArrayList<WorkspaceItem>();
        String query = "SELECT * FROM workspaceitem ORDER BY item_id";
        TableRowIterator tri = DatabaseManager.queryTable(context,
                                    "workspaceitem",
                                    query);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                WorkspaceItem wi = (WorkspaceItem) context.fromCache(
                        WorkspaceItem.class, row.getIntColumn("workspace_item_id"));

                // not in cache? turn row into workspaceitem
                if (wi == null)
                {
                    wi = new WorkspaceItem(context, row);
                }

                wsItems.add(wi);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return wsItems.toArray(new WorkspaceItem[wsItems.size()]);
    }
    
    /**
     * Get the internal ID of this workspace item
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return wiRow.getIntColumn("workspace_item_id");
    }

    /**
     * Get the value of the stage reached column
     * 
     * @return the value of the stage reached column
     */
    public int getStageReached()
    {
        return wiRow.getIntColumn("stage_reached");
    }

    /**
     * Set the value of the stage reached column
     * 
     * @param v
     *            the value of the stage reached column
     */
    public void setStageReached(int v)
    {
        wiRow.setColumn("stage_reached", v);
    }

    /**
     * Get the value of the page reached column (which represents the page
     * reached within a stage/step)
     * 
     * @return the value of the page reached column
     */
    public int getPageReached()
    {
        return wiRow.getIntColumn("page_reached");
    }

    /**
     * Set the value of the page reached column (which represents the page
     * reached within a stage/step)
     * 
     * @param v
     *            the value of the page reached column
     */
    public void setPageReached(int v)
    {
        wiRow.setColumn("page_reached", v);
    }

    /**
     * Update the workspace item, including the unarchived item.
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Authorisation is checked by the item.update() method below

        log.info(LogManager.getHeader(ourContext, "update_workspace_item",
                "workspace_item_id=" + getID()));

        // Update the item
        item.update();

        // Update ourselves
        DatabaseManager.update(ourContext, wiRow);
    }

    /**
     * Decide if this WorkspaceItem is equal to another
     *
     * @param o The other workspace item to compare to
     * @return If they are equal or not
     */
    public boolean equals(Object o) {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final WorkspaceItem that = (WorkspaceItem)o;
        if (this.getID() != that.getID())
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(getID()).toHashCode();
    }

    /**
     * Delete the workspace item. The entry in workspaceitem, the unarchived
     * item and its contents are all removed (multiple inclusion
     * notwithstanding.)
     */
    public void deleteAll() throws SQLException, AuthorizeException,
            IOException
    {
        /*
         * Authorisation is a special case. The submitter won't have REMOVE
         * permission on the collection, so our policy is this: Only the
         * original submitter or an administrator can delete a workspace item.

         */
        if (!AuthorizeManager.isAdmin(ourContext)
                && ((ourContext.getCurrentUser() == null) || (ourContext
                        .getCurrentUser().getID() != item.getSubmitter()
                        .getID())))
        {
            // Not an admit, not the submitter
            throw new AuthorizeException("Must be an administrator or the "
                    + "original submitter to delete a workspace item");
        }

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the epersongroup2workspaceitem row first since it refers
        // to workspaceitem ID
        deleteEpersonGroup2WorkspaceItem();

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);

        // Delete item
        item.delete();
    }

    private void deleteEpersonGroup2WorkspaceItem() throws SQLException
    {
        
        String removeSQL="DELETE FROM epersongroup2workspaceitem WHERE workspace_item_id = ?";
        DatabaseManager.updateQuery(ourContext, removeSQL,getID());
        
    }

    public void deleteWrapper() throws SQLException, AuthorizeException,
            IOException
    {
        // Check authorisation. We check permissions on the enclosed item.
        AuthorizeManager.authorizeAction(ourContext, item, Constants.WRITE);

        log.info(LogManager.getHeader(ourContext, "delete_workspace_item",
                "workspace_item_id=" + getID() + "item_id=" + item.getID()
                        + "collection_id=" + collection.getID()));

        //        deleteSubmitPermissions();
        // Remove from cache
        ourContext.removeCached(this, getID());

        // Need to delete the workspaceitem row first since it refers
        // to item ID
        DatabaseManager.delete(ourContext, wiRow);
    }

    // InProgressSubmission methods
    public Item getItem()
    {
        return item;
    }

    public Collection getCollection()
    {
        return collection;
    }

    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return wiRow.getBooleanColumn("multiple_files");
    }

    public void setMultipleFiles(boolean b)
    {
        wiRow.setColumn("multiple_files", b);
    }

    public boolean hasMultipleTitles()
    {
        return wiRow.getBooleanColumn("multiple_titles");
    }

    public void setMultipleTitles(boolean b)
    {
        wiRow.setColumn("multiple_titles", b);
    }

    public boolean isPublishedBefore()
    {
        return wiRow.getBooleanColumn("published_before");
    }

    public void setPublishedBefore(boolean b)
    {
        wiRow.setColumn("published_before", b);
    }
}
