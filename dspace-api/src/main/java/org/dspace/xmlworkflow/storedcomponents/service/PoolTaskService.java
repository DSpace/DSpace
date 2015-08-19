/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the PoolTask object.
 * The implementation of this class is responsible for all business logic calls for the PoolTask object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface PoolTaskService extends DSpaceCRUDService<PoolTask>
{
    public List<PoolTask> findByEperson(Context context, EPerson ePerson) throws SQLException, AuthorizeException, IOException;

    public List<PoolTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException;

    public PoolTask findByWorkflowIdAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
            throws SQLException, AuthorizeException, IOException;

    public void deleteByWorkflowItem(Context context, XmlWorkflowItem xmlWorkflowItem) throws SQLException, AuthorizeException;

    public List<PoolTask> findByEPerson(Context context, EPerson ePerson) throws SQLException;
}
