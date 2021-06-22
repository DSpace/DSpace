/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.InProgressUser;
import org.dspace.xmlworkflow.storedcomponents.InProgressUser_;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.InProgressUserDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the InProgressUser object.
 * This class is responsible for all database calls for the InProgressUser object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class InProgressUserDAOImpl extends AbstractHibernateDAO<InProgressUser> implements InProgressUserDAO {
    protected InProgressUserDAOImpl() {
        super();
    }

    @Override
    public InProgressUser findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, InProgressUser.class);
        Root<InProgressUser> inProgressUserRoot = criteriaQuery.from(InProgressUser.class);
        criteriaQuery.select(inProgressUserRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.workflowItem), workflowItem),
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.ePerson), ePerson)
                            )
        );
        return uniqueResult(context, criteriaQuery, false, InProgressUser.class, -1, -1);

    }

    @Override
    public List<InProgressUser> findByEperson(Context context, EPerson ePerson) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, InProgressUser.class);
        Root<InProgressUser> inProgressUserRoot = criteriaQuery.from(InProgressUser.class);
        criteriaQuery.select(inProgressUserRoot);
        criteriaQuery.where(criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.ePerson), ePerson));
        return list(context, criteriaQuery, false, InProgressUser.class, -1, -1);
    }

    @Override
    public List<InProgressUser> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, InProgressUser.class);
        Root<InProgressUser> inProgressUserRoot = criteriaQuery.from(InProgressUser.class);
        criteriaQuery.select(inProgressUserRoot);
        criteriaQuery.where(criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.workflowItem), workflowItem));
        return list(context, criteriaQuery, false, InProgressUser.class, -1, -1);
    }

    @Override
    public int countInProgressUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<InProgressUser> inProgressUserRoot = criteriaQuery.from(InProgressUser.class);

        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.workflowItem), workflowItem),
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.finished), false)
                            )
        );
        return count(context, criteriaQuery, criteriaBuilder, inProgressUserRoot);
    }

    @Override
    public int countFinishedUsers(Context context, XmlWorkflowItem workflowItem) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<InProgressUser> inProgressUserRoot = criteriaQuery.from(InProgressUser.class);

        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.workflowItem), workflowItem),
            criteriaBuilder.equal(inProgressUserRoot.get(InProgressUser_.finished), true)
                            )
        );
        return count(context, criteriaQuery, criteriaBuilder, inProgressUserRoot);
    }
}
