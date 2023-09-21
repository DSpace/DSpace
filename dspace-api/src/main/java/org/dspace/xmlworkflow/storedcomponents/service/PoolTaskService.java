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
import org.hibernate.Session;

/**
 * Service interface class for the PoolTask object.
 * The implementation of this class is responsible for all business logic calls for the PoolTask object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface PoolTaskService extends DSpaceCRUDService<PoolTask> {

    public List<PoolTask> find(Session session, XmlWorkflowItem workflowItem) throws SQLException;

    public List<PoolTask> findAll(Session session) throws SQLException;

    public List<PoolTask> findByEPerson(Session session, EPerson ePerson) throws SQLException;

    public List<PoolTask> findByEperson(Context context, EPerson ePerson)
        throws SQLException, AuthorizeException, IOException;

    public PoolTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException, AuthorizeException, IOException;

    public void deleteByWorkflowItem(Context context, XmlWorkflowItem xmlWorkflowItem)
        throws SQLException, AuthorizeException;

    public void deleteByEperson(Context context, EPerson ePerson) throws SQLException, AuthorizeException, IOException;

    /**
     * This method will return a list of PoolTask for the given group.
     * @param session   current request's database context.
     * @param group     The Group to be searched on
     * @return          The list of PoolTask objects
     * @throws SQLException If something goes wrong
     */
    public List<PoolTask> findByGroup(Session session, Group group) throws SQLException;
}
