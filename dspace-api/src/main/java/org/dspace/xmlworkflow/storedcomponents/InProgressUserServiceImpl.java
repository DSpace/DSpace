/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.dao.InProgressUserDAO;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;

/**
 * Service implementation for the InProgressUser object.
 * This class is responsible for all business logic calls for the InProgressUser object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class InProgressUserServiceImpl implements InProgressUserService {

    @Autowired(required = true)
    protected InProgressUserDAO inProgressUserDAO;

    protected InProgressUserServiceImpl()
    {

    }

    @Override
    public InProgressUser findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson) throws SQLException {
        return inProgressUserDAO.findByWorkflowItemAndEPerson(context, workflowItem, ePerson);
    }

    @Override
    public List<InProgressUser> findByEperson(Context context, EPerson ePerson) throws SQLException {
        return inProgressUserDAO.findByEperson(context, ePerson);
    }

    @Override
    public List<InProgressUser> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.findByWorkflowItem(context, workflowItem);
    }

    @Override
    public int getNumberOfInProgressUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.countInProgressUsers(context, workflowItem);
    }

    @Override
    public int getNumberOfFinishedUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.countFinishedUsers(context, workflowItem);
    }

    @Override
    public InProgressUser create(Context context) throws SQLException, AuthorizeException {
        return inProgressUserDAO.create(context, new InProgressUser());
    }

    @Override
    public InProgressUser find(Context context, int id) throws SQLException {
        return inProgressUserDAO.findByID(context, InProgressUser.class, id);
    }

    @Override
    public void update(Context context, InProgressUser inProgressUser) throws SQLException, AuthorizeException {
        inProgressUserDAO.save(context, inProgressUser);
    }

    @Override
    public void delete(Context context, InProgressUser inProgressUser) throws SQLException, AuthorizeException {
        inProgressUserDAO.delete(context, inProgressUser);
    }
}
