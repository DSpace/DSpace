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
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask_;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.dao.PoolTaskDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the PoolTask object.
 * This class is responsible for all database calls for the PoolTask object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class PoolTaskDAOImpl extends AbstractHibernateDAO<PoolTask> implements PoolTaskDAO {
    protected PoolTaskDAOImpl() {
        super();
    }

    @Override
    public List<PoolTask> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, PoolTask.class);
        Root<PoolTask> poolTaskRoot = criteriaQuery.from(PoolTask.class);
        criteriaQuery.select(poolTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.ePerson), ePerson));
        return list(context, criteriaQuery, false, PoolTask.class, -1, -1);

    }

    @Override
    public List<PoolTask> findByGroup(Context context, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, PoolTask.class);
        Root<PoolTask> poolTaskRoot = criteriaQuery.from(PoolTask.class);
        criteriaQuery.select(poolTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.group), group));
        return list(context, criteriaQuery, false, PoolTask.class, -1, -1);
    }

    @Override
    public List<PoolTask> findByWorkflowItem(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, PoolTask.class);
        Root<PoolTask> poolTaskRoot = criteriaQuery.from(PoolTask.class);
        criteriaQuery.select(poolTaskRoot);
        criteriaQuery.where(criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.workflowItem), workflowItem));
        return list(context, criteriaQuery, false, PoolTask.class, -1, -1);
    }

    @Override
    public PoolTask findByWorkflowItemAndEPerson(Context context, XmlWorkflowItem workflowItem, EPerson ePerson)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, PoolTask.class);
        Root<PoolTask> poolTaskRoot = criteriaQuery.from(PoolTask.class);
        criteriaQuery.select(poolTaskRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.workflowItem), workflowItem),
                                       criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.ePerson), ePerson)
                   )
        );
        return uniqueResult(context, criteriaQuery, false, PoolTask.class, -1, -1);
    }

    @Override
    public PoolTask findByWorkflowItemAndGroup(Context context, Group group, XmlWorkflowItem workflowItem)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, PoolTask.class);
        Root<PoolTask> poolTaskRoot = criteriaQuery.from(PoolTask.class);
        criteriaQuery.select(poolTaskRoot);
        criteriaQuery
            .where(criteriaBuilder.and(criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.workflowItem), workflowItem),
                                       criteriaBuilder.equal(poolTaskRoot.get(PoolTask_.group), group)
                   )
        );
        return uniqueResult(context, criteriaQuery, false, PoolTask.class, -1, -1);
    }
}
