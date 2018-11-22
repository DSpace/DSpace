/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.WorkspaceItem_;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson_;
import org.dspace.eperson.Group;

/**
 * Hibernate implementation of the Database Access Object interface class for the WorkspaceItem object.
 * This class is responsible for all database calls for the WorkspaceItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemDAOImpl extends AbstractHibernateDAO<WorkspaceItem> implements WorkspaceItemDAO {
    protected WorkspaceItemDAOImpl() {
        super();
    }


    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep) throws SQLException {
        Query query = createQuery(context,
                                  "from WorkspaceItem ws where ws.item.submitter = :submitter order by " +
                                      "workspaceItemId");
        query.setParameter("submitter", ep);
        return list(query);
    }

    @Override
    public List<WorkspaceItem> findByEPerson(Context context, EPerson ep, Integer limit, Integer offset)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.item).get("submitter"), ep));
        criteriaQuery.orderBy(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        return list(context, criteriaQuery, false, WorkspaceItem.class, limit, offset);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Context context, Collection c) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.collection), c));
        criteriaQuery.orderBy(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public WorkspaceItem findByItem(Context context, Item i) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.item), i));
        return uniqueResult(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public List<WorkspaceItem> findAll(Context context, Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, WorkspaceItem.class, limit, offset);
    }

    @Override
    public List<WorkspaceItem> findWithSupervisedGroup(Context context) throws SQLException {
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
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        Join<WorkspaceItem, Group> join = workspaceItemRoot.join("supervisorGroups");
        Join<Group, EPerson> secondJoin = join.join("epeople");
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(secondJoin.get(EPerson_.id), ePerson.getID()));
        criteriaQuery.orderBy(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        return list(context, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) from WorkspaceItem"));
    }

    @Override
    public int countRows(Context context, EPerson ep) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT count(*) from WorkspaceItem ws where ws.item.submitter = :submitter");
        query.setParameter("submitter", ep);
        return count(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Context context) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT wi.stageReached as stage_reached, count(*) as cnt from WorkspaceItem wi" +
                                      " group by wi.stageReached order by wi.stageReached");

        List<Object[]> list = query.getResultList();
        List<Map.Entry<Integer, Long>> returnList = new LinkedList<>();
        for (Object[] o : list) {
            returnList.add(new AbstractMap.SimpleEntry<>((Integer) o[0], (Long) o[1]));
        }
        return returnList;
    }

}
