/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.service;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflowbasic.BasicWorkflowItem;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the BasicWorkflowItem object.
 * The implementation of this class is responsible for all business logic calls for the BasicWorkflowItem object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BasicWorkflowItemService extends WorkflowItemService<BasicWorkflowItem>
{

    public List<BasicWorkflowItem> findPooledTasks(Context context, EPerson ePerson) throws SQLException;

    /**
     * Retrieve the list of BasicWorkflowItems that the given EPerson is owner of (owner == claimed for review)
     * @param context
     *     The relevant DSpace Context.
     * @param ePerson
     *     The DSpace EPerson object.
     * @return a list of BasicWorkflowItem objects
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<BasicWorkflowItem> findByOwner(Context context, EPerson ePerson) throws SQLException;

    int countTotal(Context context) throws SQLException;
}
