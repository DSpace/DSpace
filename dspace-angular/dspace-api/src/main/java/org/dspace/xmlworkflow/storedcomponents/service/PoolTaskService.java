/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Service interface class for the PoolTask object.
 * The implementation of this class is responsible for all business logic calls for the PoolTask object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface PoolTaskService extends DSpaceCRUDService<PoolTask> {

    public List<PoolTask> findAll(Context context) throws SQLException;

    public List<PoolTask> findByEperson(Context context, EPerson ePerson)
        throws SQLException, AuthorizeException, IOException;

    public List<PoolTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public PoolTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException, AuthorizeException, IOException;

    public void deleteByWorkflowItem(Context context, XmlWorkflowItem xmlWorkflowItem)
        throws SQLException, AuthorizeException;

    public void deleteByEperson(Context context, EPerson ePerson) throws SQLException, AuthorizeException, IOException;

    public List<PoolTask> findByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * This method will return a list of PoolTask for the given group
     * @param context   The relevant DSpace context
     * @param group     The Group to be searched on
     * @return          The list of PoolTask objects
     * @throws SQLException If something goes wrong
     */
    public List<PoolTask> findByGroup(Context context, Group group) throws SQLException;
}
