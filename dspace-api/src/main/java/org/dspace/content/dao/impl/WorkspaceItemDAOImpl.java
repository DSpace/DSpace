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
import javax.persistence.criteria.Root;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.WorkspaceItem_;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.EPerson;
import org.hibernate.Session;

/**
 * Hibernate implementation of the Database Access Object interface class for the WorkspaceItem object.
 * This class is responsible for all database calls for the WorkspaceItem object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkspaceItemDAOImpl extends AbstractHibernateDAO<WorkspaceItem> implements WorkspaceItemDAO {
    protected WorkspaceItemDAOImpl() {
        super();
    }


    @Override
    public List<WorkspaceItem> findByEPerson(Session session, EPerson ep) throws SQLException {
        Query query = createQuery(session,
                                  "from WorkspaceItem ws where ws.item.submitter = :submitter order by " +
                                      "workspaceItemId");
        query.setParameter("submitter", ep);
        return list(query);
    }

    @Override
    public List<WorkspaceItem> findByEPerson(Session session, EPerson ep, Integer limit, Integer offset)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.item).get("submitter"), ep));
        criteriaQuery.orderBy(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        return list(session, criteriaQuery, false, WorkspaceItem.class, limit, offset);
    }

    @Override
    public List<WorkspaceItem> findByCollection(Session session, Collection c) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.collection), c));
        criteriaQuery.orderBy(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        return list(session, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public WorkspaceItem findByItem(Session session, Item i) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(workspaceItemRoot.get(WorkspaceItem_.item), i));
        return uniqueResult(session, criteriaQuery, false, WorkspaceItem.class);
    }

    @Override
    public List<WorkspaceItem> findAll(Session session) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        criteriaQuery.orderBy(orderList);
        return list(session, criteriaQuery, false, WorkspaceItem.class, -1, -1);
    }

    @Override
    public List<WorkspaceItem> findAll(Session session, Integer limit, Integer offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, WorkspaceItem.class);
        Root<WorkspaceItem> workspaceItemRoot = criteriaQuery.from(WorkspaceItem.class);
        criteriaQuery.select(workspaceItemRoot);

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(workspaceItemRoot.get(WorkspaceItem_.workspaceItemId)));
        criteriaQuery.orderBy(orderList);


        return list(session, criteriaQuery, false, WorkspaceItem.class, limit, offset);
    }

    @Override
    public int countRows(Session session) throws SQLException {
        return count(createQuery(session, "SELECT count(*) from WorkspaceItem"));
    }

    @Override
    public int countRows(Session session, EPerson ep) throws SQLException {
        Query query = createQuery(session,
                                  "SELECT count(*) from WorkspaceItem ws where ws.item.submitter = :submitter");
        query.setParameter("submitter", ep);
        return count(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map.Entry<Integer, Long>> getStageReachedCounts(Session session) throws SQLException {
        Query query = createQuery(session,
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
