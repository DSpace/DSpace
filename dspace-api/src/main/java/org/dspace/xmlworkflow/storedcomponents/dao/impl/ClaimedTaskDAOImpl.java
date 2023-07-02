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
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask_;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.ClaimedTaskDAO;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the ClaimedTask object.
 * This class is responsible for all database calls for the ClaimedTask object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ClaimedTaskDAOImpl extends AbstractHibernateDAO<ClaimedTask> implements ClaimedTaskDAO {
    protected ClaimedTaskDAOImpl() {
        super();
    }

    @Override
    public List<ClaimedTask> findByWorkflowItem(Session session, XmlWorkflowItem workflowItem) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.workflowItem), workflowItem));
        return list(session, criteriaQuery, false, ClaimedTask.class, -1, -1);
    }

    @Override
    public ClaimedTask findByWorkflowItemAndEPerson(Session session, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.workflowItem), workflowItem),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.owner), ePerson)
            )
        );
        return uniqueResult(session, criteriaQuery, false, ClaimedTask.class);
    }

    @Override
    public List<ClaimedTask> findByEperson(Session session, EPerson ePerson) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.owner), ePerson));
        return list(session, criteriaQuery, false, ClaimedTask.class, -1, -1);
    }

    @Override
    public List<ClaimedTask> findByWorkflowItemAndStepId(Session session, XmlWorkflowItem workflowItem, String stepID)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.workflowItem), workflowItem),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.stepId), stepID)
            )
        );
        return list(session, criteriaQuery, false, ClaimedTask.class, -1, -1);
    }

    @Override
    public ClaimedTask findByEPersonAndWorkflowItemAndStepIdAndActionId(Session session, EPerson ePerson,
                                                                        XmlWorkflowItem workflowItem, String stepID,
                                                                        String actionID) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.workflowItem), workflowItem),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.stepId), stepID),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.owner), ePerson),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.actionId), actionID)
            )
        );
        return uniqueResult(session, criteriaQuery, false, ClaimedTask.class);
    }

    @Override
    public List<ClaimedTask> findByWorkflowItemAndStepIdAndActionId(Session session, XmlWorkflowItem workflowItem,
                                                                    String stepID, String actionID)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(
            criteriaBuilder.and(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.workflowItem), workflowItem),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.stepId), stepID),
                                criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.actionId), actionID)
            )
        );
        return list(session, criteriaQuery, false, ClaimedTask.class, -1, -1);
    }

    @Override
    public List<ClaimedTask> findByStep(Session session, String stepID) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClaimedTask.class);
        Root<ClaimedTask> claimedTaskRoot = criteriaQuery.from(ClaimedTask.class);
        criteriaQuery.select(claimedTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(claimedTaskRoot.get(ClaimedTask_.stepId), stepID));
        return list(session, criteriaQuery, false, ClaimedTask.class, -1, -1);
    }
}
