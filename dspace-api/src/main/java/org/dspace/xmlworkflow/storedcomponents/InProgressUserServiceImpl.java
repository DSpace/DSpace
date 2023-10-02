/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.dao.InProgressUserDAO;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

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

    protected InProgressUserServiceImpl() {

    }

    @Override
    public InProgressUser findByWorkflowItemAndEPerson(Session session, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException {
        return inProgressUserDAO.findByWorkflowItemAndEPerson(session, workflowItem, ePerson);
    }

    @Override
    public List<InProgressUser> findByEperson(Session session, EPerson ePerson) throws SQLException {
        return inProgressUserDAO.findByEperson(session, ePerson);
    }

    @Override
    public List<InProgressUser> findByWorkflowItem(Session session, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.findByWorkflowItem(session, workflowItem);
    }

    @Override
    public int getNumberOfInProgressUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.countInProgressUsers(context.getSession(), workflowItem);
    }

    @Override
    public int getNumberOfFinishedUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        return inProgressUserDAO.countFinishedUsers(context.getSession(), workflowItem);
    }

    @Override
    public InProgressUser create(Context context) throws SQLException, AuthorizeException {
        return inProgressUserDAO.create(context.getSession(), new InProgressUser());
    }

    @Override
    public InProgressUser find(Session session, int id) throws SQLException {
        return inProgressUserDAO.findByID(session, InProgressUser.class, id);
    }

    @Override
    public void update(Context context, InProgressUser inProgressUser) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(inProgressUser));
    }

    @Override
    public void update(Context context, List<InProgressUser> inProgressUsers) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(inProgressUsers)) {
            for (InProgressUser inProgressUser : inProgressUsers) {
                inProgressUserDAO.save(context.getSession(), inProgressUser);
            }
        }
    }

    @Override
    public void delete(Context context, InProgressUser inProgressUser) throws SQLException, AuthorizeException {
        inProgressUserDAO.delete(context.getSession(), inProgressUser);
    }
}
