/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Database Access Object interface class for the XmlWorkflowItem object.
 * The implementation of this class is responsible for all database calls for the XmlWorkflowItem object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface XmlWorkflowItemDAO extends GenericDAO<XmlWorkflowItem> {

    /**
     * Find all the workflow items in a specific collection using the pagination parameters (offset, limit)
     * 
     * @param context
     *            dspace context
     * @param offset
     *            the first record to return
     * @param limit
     *            the max number of records to return
     * @param collection
     *            the collection where the workflowitem has been submitted
     * @return all the workflow items respecting the parameters conditions
     * @throws SQLException
     */
    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer offset, Integer limit,
                                                     Collection collection) throws SQLException;

    public int countAll(Context context) throws SQLException;

    public int countAllInCollection(Context context, Collection collection) throws SQLException;

    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException;

    public List<XmlWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException;

    public XmlWorkflowItem findByItem(Context context, Item item) throws SQLException;

    /**
     * Return all the workflow items from a specific submitter respecting the pagination parameters
     * 
     * @param context
     *            The relevant DSpace Context.
     * @param ep
     *            the eperson that has submitted the item
     * @param offset
     *            the first record to return
     * @param limit
     *            the max number of records to return
     * @return
     */
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep, Integer offset, Integer limit)
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
