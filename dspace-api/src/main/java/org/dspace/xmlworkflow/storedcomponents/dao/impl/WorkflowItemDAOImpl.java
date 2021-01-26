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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Item_;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItem_;
import org.dspace.xmlworkflow.storedcomponents.dao.WorkflowItemDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the WorkflowItem object.
 * This class is responsible for all database calls for the WorkflowItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkflowItemDAOImpl extends AbstractHibernateDAO<WorkflowItem> implements WorkflowItemDAO {

    protected WorkflowItemDAOImpl() {
        super();
    }

    @Override
    public List<WorkflowItem> findAllInCollection(Context context, Integer offset,
                                                  Integer limit,
                                                  Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkflowItem.class);
        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        criteriaQuery.select(workflowItemRoot);
        if (collection != null) {
            criteriaQuery.where(criteriaBuilder.equal(workflowItemRoot.get(WorkflowItem_.collection),
                                                      collection));
        }
        if (offset == null) {
            offset = -1;
        }
        if (limit == null) {
            limit = -1;
        }
        criteriaQuery.orderBy(criteriaBuilder.asc(workflowItemRoot.get(WorkflowItem_.id)));
        return list(context, criteriaQuery, false, WorkflowItem.class, limit, offset);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return countAllInCollection(context, null);
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        if (collection != null) {
            criteriaQuery.where(criteriaBuilder.equal(workflowItemRoot.get(WorkflowItem_.collection),
                                                      collection));
        }
        return count(context, criteriaQuery, criteriaBuilder, workflowItemRoot);
    }

    @Override
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        return findBySubmitter(context, ep, null, null);
    }

    @Override
    public List<WorkflowItem> findBySubmitter(Context context, EPerson ep, Integer offset, Integer limit)
            throws SQLException {
        if (offset == null) {
            offset = -1;
        }
        if (limit == null) {
            limit = -1;
        }

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkflowItem.class);
        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        Join<WorkflowItem, Item> join = workflowItemRoot.join("item");
        criteriaQuery.select(workflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(Item_.submitter), ep));
        criteriaQuery.orderBy(criteriaBuilder.asc(workflowItemRoot.get(WorkflowItem_.id)));
        return list(context, criteriaQuery, false, WorkflowItem.class, limit, offset);
    }

    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        Join<WorkflowItem, Item> join = workflowItemRoot.join("item");
        criteriaQuery.where(criteriaBuilder.equal(join.get(Item_.submitter), ep));
        return count(context, criteriaQuery, criteriaBuilder, workflowItemRoot);
    }

    @Override
    public List<WorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkflowItem.class);
        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        criteriaQuery.select(workflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workflowItemRoot.get(WorkflowItem_.collection), collection));
        criteriaQuery.orderBy(criteriaBuilder.asc(workflowItemRoot.get(WorkflowItem_.id)));
        return list(context, criteriaQuery, false, WorkflowItem.class, -1, -1);
    }

    @Override
    public WorkflowItem findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkflowItem.class);
        Root<WorkflowItem> workflowItemRoot = criteriaQuery.from(WorkflowItem.class);
        criteriaQuery.select(workflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workflowItemRoot.get(WorkflowItem_.item), item));
        return uniqueResult(context, criteriaQuery, false, WorkflowItem.class, -1, -1);
    }
}
