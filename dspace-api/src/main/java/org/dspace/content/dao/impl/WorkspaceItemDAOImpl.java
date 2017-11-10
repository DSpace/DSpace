/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.WorkspaceItem_;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson_;
import org.dspace.eperson.Group;
import org.dspace.harvest.HarvestedCollection_;
import org.dspace.identifier.DOI;
import org.dspace.workflow.WorkflowItem;
import org.hibernate.Criteria;
import javax.persistence.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.BasicTransformerAdapter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of the Database Access Object interface class for the WorkspaceItem object.
 * This class is responsible for all database calls for the WorkspaceItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemDAOImpl extends AbstractHibernateDAO<WorkspaceItem> implements WorkspaceItemDAO
{
    protected WorkspaceItemDAOImpl()
    {
        super();
    }


    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException
    {
        Query query = createQuery(context, "from WorkspaceItem ws where ws.item.submitter = :submitter order by workspaceItemId");
        query.setParameter("submitter", ep);
        return list(query);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Context context, Collection c) throws SQLException
    {
//        Criteria criteria = createCriteria(context, WorkspaceItem.class);
//        criteria.add(Restrictions.eq("collection", c));
//        return list(criteria);

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.collection), c));
        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);

    }

    @Override
    public WorkspaceItem findByItem(Context context, Item i) throws SQLException
    {
//        Criteria criteria = createCriteria(context, WorkspaceItem.class);
//        criteria.add(Restrictions.eq("item", i));
//         Look for the unique workspaceitem entry where 'item_id' references this item
//        return uniqueResult(criteria);
//

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.item), i));
        return uniqueResult(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context) throws SQLException
    {
        //TODO RAF CHECK
//        Criteria criteria = createCriteria(context, WorkspaceItem.class);
//        criteria.addOrder(Order.asc("item"));
//        return list(criteria);

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.item)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }
    
    @Override
    public List<WorkspaceItem> findAll(Context context, Integer limit, Integer offset) throws SQLException
    {
        Criteria criteria = createCriteria(context, WorkspaceItem.class);
        criteria.addOrder(Order.asc("item"));
        criteria.setFirstResult(offset);
        criteria.setMaxResults(limit);
        return list(criteria);
    }

    @Override
    public List<WorkspaceItem> findWithSupervisedGroup(Context context) throws SQLException {
        //TODO RAF CHECK
//        Criteria criteria = createCriteria(context, WorkspaceItem.class);
//        criteria.add(Restrictions.isNotEmpty("supervisorGroups"));
//        criteria.addOrder(Order.asc("workspaceItemId"));
//        return list(criteria);

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.isNotEmpty(workspaceItemRoot.get(WorkspaceItem_.supervisorGroups)));

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);


    }

    @Override
    public List<WorkspaceItem> findBySupervisedGroupMember(Context context, EPerson ePerson) throws SQLException {

        //TODO RAF CHECK
//        Criteria criteria = createCriteria(context, WorkspaceItem.class);
//        criteria.createAlias("supervisorGroups", "supervisorGroup");
//        criteria.createAlias("supervisorGroup.epeople", "person");
//        criteria.add(Restrictions.eq("person.id", ePerson.getID()));
//        return list(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        Join<WorkspaceItem, Group> join = workspaceItemRoot.join("supervisorGroups");
        Join<Group, EPerson> secondJoin = join.join("epeople");
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(secondJoin.get(EPerson_.id), ePerson.getID()));
        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from WorkspaceItem"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException {
        Query query = createQuery(context,"SELECT wi.stageReached as stage_reached, count(*) as cnt from WorkspaceItem wi" +
                " group by wi.stageReached order by wi.stageReached");

        //TODO RAF WRITE
//        query.setResultTransformer(new BasicTransformerAdapter() {
//            @Override
//            public Object transformTuple(Object[] tuple, String[] aliases) {
//                return new java.util.AbstractMap.SimpleImmutableEntry((Integer) tuple[0], (Long) tuple[1]);
//            }
//        });
        return (List<Map.Entry<Integer, Long>>)query.getResultList();
    }

}
