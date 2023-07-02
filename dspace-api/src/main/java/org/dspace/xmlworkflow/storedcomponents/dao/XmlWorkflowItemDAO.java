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
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hibernate.Session;

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
     * @param session
     *            the current request's database context.
     * @param offset
     *            the first record to return
     * @param limit
     *            the max number of records to return
     * @param collection
     *            the collection where the workflowitem has been submitted
     * @return all the workflow items respecting the parameters conditions
     * @throws SQLException
     */
    public List<XmlWorkflowItem> findAllInCollection(Session session, Integer offset, Integer limit,
                                                     Collection collection) throws SQLException;

    public int countAll(Session session) throws SQLException;

    public int countAllInCollection(Session session, Collection collection) throws SQLException;

    public List<XmlWorkflowItem> findBySubmitter(Session session, EPerson ep) throws SQLException;

    public List<XmlWorkflowItem> findByCollection(Session session, Collection collection) throws SQLException;

    public XmlWorkflowItem findByItem(Session session, Item item) throws SQLException;

    /**
     * Return all the workflow items from a specific submitter respecting the pagination parameters
     *
     * @param session
     *            The current request's database context.
     * @param ep
     *            the eperson that has submitted the item
     * @param offset
     *            the first record to return
     * @param limit
     *            the max number of records to return
     * @return
     * @throws SQLException
     */
    public List<XmlWorkflowItem> findBySubmitter(Session session, EPerson ep, Integer offset, Integer limit)
            throws SQLException;

    /**
     * Count the number of workflow items from a specific submitter
     *
     * @param session
     *            The current request's database context.
     * @param ep
     *            the eperson that has submitted the item
     * @return
     * @throws SQLException
     */
    public int countBySubmitter(Session session, EPerson ep) throws SQLException;
}
