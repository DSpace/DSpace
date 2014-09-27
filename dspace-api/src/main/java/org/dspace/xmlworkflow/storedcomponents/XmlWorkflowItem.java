/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
import org.apache.log4j.Logger;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.XmlWorkflowManager;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.io.IOException;

/**
 * Class representing an item going through the workflow process in DSpace
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XmlWorkflowItem implements InProgressSubmission {

    /*
     * The current step in the workflow system in which this workflow item is present
     */
    private static final Logger log = Logger.getLogger(XmlWorkflowItem.class);

    private Collection collection;

    private Item item;

    private final TableRow wfRow;

    private final Context ourContext;


    public static XmlWorkflowItem create(Context context) throws AuthorizeException, IOException, SQLException {
        TableRow row = DatabaseManager.create(context, "cwf_workflowitem");

        return new XmlWorkflowItem(context, row);
    }

    /*
     * In the case where multiple epersons can claim and perform an action, this map will represent the progress
     * of each of these epersons in the step
     */
//    private ArrayList<StepRecord> activeSteps;

    XmlWorkflowItem(Context context, TableRow row) throws SQLException {
        ourContext = context;
        wfRow = row;
 //       activeSteps = new ArrayList<StepRecord>();

        item = Item.find(context, wfRow.getIntColumn("item_id"));
        collection = Collection.find(context, wfRow.getIntColumn("collection_id"));
        // Cache ourselves
        context.cache(this, row.getIntColumn("workflowitem_id"));
//        initialize();
//        update();
    }

    /**
     * Get a workflow item from the database. The item, collection and submitter
     * are loaded into memory.
     *
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the workspace item
     *
     * @return the workflow item, or null if the ID is invalid.
     */
    public static XmlWorkflowItem find(Context context, int id)
            throws SQLException, AuthorizeException, IOException {
        // First check the cache
        XmlWorkflowItem fromCache = (XmlWorkflowItem) context.fromCache(
                XmlWorkflowItem.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "cwf_workflowitem", id);
//        TableRow row = DatabaseManager.querySingle(context, "SELECT * FROM workflowitem WHERE item_id= "+id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "not_found,workflowitem_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_workflow_item",
                        "workflowitem_id=" + id));
            }

            return new XmlWorkflowItem(context, row);
        }
    }

    /**
     * return all workflowitems
     *
     * @param c  active context
     * @return WorkflowItem [] of all workflows in system
     */
    public static XmlWorkflowItem[] findAll(Context c) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        List wfItems = new ArrayList();
        TableRowIterator tri = DatabaseManager.queryTable(c, "cwf_workflowitem",
                "SELECT * FROM cwf_workflowitem");

        try
        {
            // make a list of workflow items
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                XmlWorkflowItem wi = new XmlWorkflowItem(c, row);
                wfItems.add(wi);
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }

        XmlWorkflowItem[] wfArray = new XmlWorkflowItem[wfItems.size()];
        wfArray = (XmlWorkflowItem[]) wfItems.toArray(wfArray);

        return wfArray;
    }

    /**
     * return all workflowitems for a certain page
     *
     * @param c  active context
     * @return WorkflowItem [] of all workflows in system
     */
    public static XmlWorkflowItem[] findAll(Context c, int page, int pagesize) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        return findAllInCollection(c, page, pagesize, -1);
    }

    /**
     * return all workflowitems for a certain page with a certain collection
     *
     * @param c  active context
     * @return WorkflowItem [] of all workflows in system
     */
    public static XmlWorkflowItem[] findAllInCollection(Context c, int page, int pagesize, int collectionId) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        List wfItems = new ArrayList();
        StringBuffer query = new StringBuffer();

        query.append("SELECT * FROM cwf_workflowitem ");
        if(collectionId != -1){
            query.append("WHERE collection_id=").append(collectionId);
        }
        int offset = (page - 1) * pagesize;
        if (DatabaseManager.isOracle()) {
            // First prepare the query to generate row numbers
            if (pagesize > 0 || offset > 0) {
                query.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                query.append(") ");
            }
            // Restrict the number of rows returned based on the limit
            if (pagesize > 0) {
                query.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0) {
                    pagesize += offset;
                }
            }
            // Return only the records after the specified offset (row number)
            if (offset > 0) {
                query.insert(0, "SELECT * FROM (");
                query.append(") WHERE rnum>?");
            }
        } else {
            if (pagesize > 0) {
                query.append(" LIMIT ? ");
            }
            if (offset > 0) {
                query.append(" OFFSET ? ");
            }
        }
        String queryString = query.toString();
        Object[] paramArr = new Object[]{};
        if (pagesize > 0 && offset > 0) {
            paramArr = new Object[]{pagesize, offset};
        } else if (pagesize > 0) {
            paramArr = new Object[]{pagesize};
        } else if (offset > 0) {
            paramArr = new Object[]{offset};
        }
        // Get all the epeople that match the query
        TableRowIterator tri = DatabaseManager.query(c, queryString, paramArr);
        try
        {
            // make a list of workflow items
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                XmlWorkflowItem wi = new XmlWorkflowItem(c, row);
                wfItems.add(wi);
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }

        XmlWorkflowItem[] wfArray = new XmlWorkflowItem[wfItems.size()];
        wfArray = (XmlWorkflowItem[]) wfItems.toArray(wfArray);

        return wfArray;
    }


    /**
         * return all workflowitems
         *
         * @param c  active context
         * @return WorkflowItem [] of all workflows in system
         */
        public static int countAll(Context c) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
            return countAllInCollection(c, -1);
        }

    /**
         * return all workflowitems
         *
         * @param c  active context
         * @return WorkflowItem [] of all workflows in system
         */
        public static int countAllInCollection(Context c, int collId) throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
        StringBuffer query = new StringBuffer();
        query.append("SELECT count(*) AS count FROM cwf_workflowitem ");
        if(collId != -1){
            query.append(" WHERE collection_id= ").append(collId);
        }

        TableRow tr = DatabaseManager.querySingle(c,query.toString());

            return new Long(tr.getLongColumn("count")).intValue();
        }


    /*
     * Returns all workflow items submitted by an eperson
     */
    public static XmlWorkflowItem[] findByEPerson(Context context, EPerson ep)
            throws SQLException, AuthorizeException, IOException, WorkflowConfigurationException {
            List wfItems = new ArrayList();

            TableRowIterator tri = DatabaseManager.queryTable(context, "cwf_workflowitem",
                    "SELECT cwf_workflowitem.* FROM cwf_workflowitem, item WHERE " +
                    "cwf_workflowitem.item_id=item.item_id AND " +
                    "item.submitter_id= ? " +
                    "ORDER BY cwf_workflowitem.workflowitem_id",
                    ep.getID());

            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                XmlWorkflowItem wi = (XmlWorkflowItem) context.fromCache(
                        XmlWorkflowItem.class, row.getIntColumn("workflowitem_id"));

                if (wi == null)
                {
                    wi = new XmlWorkflowItem(context, row);
                }

                wfItems.add(wi);
            }

            tri.close();

            XmlWorkflowItem[] wfArray = new XmlWorkflowItem[wfItems.size()];
            wfArray = (XmlWorkflowItem[]) wfItems.toArray(wfArray);

            return wfArray;
    }

    /**
     * Get all workflow items for a particular collection.
     *
     * @param context
     *            the context object
     * @param c
     *            the collection
     *
     * @return array of the corresponding workflow items
     */
    public static XmlWorkflowItem[] findByCollection(Context context, Collection c)
            throws SQLException, AuthorizeException, IOException {
        List wsItems = new ArrayList();

        TableRowIterator tri = DatabaseManager.queryTable(context, "cwf_workflowitem",
                "SELECT cwf_workflowitem.* FROM cwf_workflowitem WHERE " +
                "cwf_workflowitem.collection_id= ? ",
                c.getID());

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Check the cache
                XmlWorkflowItem wi = (XmlWorkflowItem) context.fromCache(
                        XmlWorkflowItem.class, row.getIntColumn("workflowitem_id"));

                // not in cache? turn row into workflowitem
                if (wi == null)
                {
                    wi = new XmlWorkflowItem(context, row);
                }

                wsItems.add(wi);
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }
        XmlWorkflowItem[] wsArray = new XmlWorkflowItem[wsItems.size()];
        wsArray = (XmlWorkflowItem[]) wsItems.toArray(wsArray);

        return wsArray;
    }

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
    public static XmlWorkflowItem findByItem(Context context, Item item) throws SQLException{
        TableRow row = DatabaseManager.findByUnique(context, "cwf_workflowitem", "item_id", item.getID());

        XmlWorkflowItem wi = null;
        if(row != null){
            // Check the cache
            wi = (XmlWorkflowItem) context.fromCache(XmlWorkflowItem.class, row.getIntColumn("workflowitem_id"));

            // not in cache? turn row into workflowitem
            if (wi == null)
            {
                wi = new XmlWorkflowItem(context, row);
            }
        }
        return wi;
    }

    /**
     * Update the workflow item, including the unarchived item.
     */
    public void update() throws SQLException, AuthorizeException {
        // FIXME check auth
        log.info(LogManager.getHeader(ourContext, "update_workflow_item",
                "workflowitem_id=" + getID()));

        // Update the item
        item.update();

        // Update ourselves
        DatabaseManager.update(ourContext, wfRow);
    }


    /**
     * Get the internal ID of this workflow item
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return wfRow.getIntColumn("workflowitem_id");
    }


    public Collection getCollection(){
        return this.collection;
    }

    public void setCollection(Collection collection){
        this.collection = collection;
        wfRow.setColumn("collection_id", collection.getID());
    }

    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item){
        this.item = item;
        wfRow.setColumn("item_id", item.getID());
    }

    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return wfRow.getBooleanColumn("multiple_files");
    }

    public void setMultipleFiles(boolean b)
    {
        wfRow.setColumn("multiple_files", b);
    }

    public boolean hasMultipleTitles()
    {
        return wfRow.getBooleanColumn("multiple_titles");
    }

    public void setMultipleTitles(boolean b)
    {
        wfRow.setColumn("multiple_titles", b);
    }

    public boolean isPublishedBefore()
    {
        return wfRow.getBooleanColumn("published_before");
    }

    public void setPublishedBefore(boolean b)
    {
        wfRow.setColumn("published_before", b);
    }

    /**
     * delete the WorkflowItem, retaining the Item
     */
    public void deleteWrapper() throws SQLException, IOException,
            AuthorizeException
    {
        // Remove from cache
        ourContext.removeCached(this, getID());

        WorkflowItemRole[] roles = WorkflowItemRole.findAllForItem(ourContext, this.getID());
        for(WorkflowItemRole role: roles){
            role.delete();
        }
        XmlWorkflowManager.deleteAllTasks(ourContext, this);

        // FIXME - auth?
        DatabaseManager.delete(ourContext, wfRow);
    }
}
