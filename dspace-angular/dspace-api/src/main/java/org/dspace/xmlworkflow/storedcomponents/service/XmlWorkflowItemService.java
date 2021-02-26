/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Service interface class for the XmlWorkflowItem object.
 * The implementation of this class is responsible for all business logic calls for the XmlWorkflowItem object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface XmlWorkflowItemService extends WorkflowItemService<XmlWorkflowItem> {

    /**
     * return all workflowitems for a certain page
     *
     * @param context  The relevant DSpace Context.
     * @param page     paging: page number
     * @param pagesize paging: items per page
     * @return WorkflowItem list of all the workflow items in system
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<XmlWorkflowItem> findAll(Context context, Integer page, Integer pagesize) throws SQLException;

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
    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer page, Integer pagesize,
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
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep, Integer pageNumber, Integer pageSize)
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
