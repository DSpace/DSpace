/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Query;
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
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.BasicWorkflowItem_;
import org.dspace.workflowbasic.dao.BasicWorkflowItemDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the BasicWorkflowItem object.
 * This class is responsible for all database calls for the BasicWorkflowItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class BasicWorkflowItemDAOImpl extends AbstractHibernateDAO<BasicWorkflowItem> implements BasicWorkflowItemDAO {
    protected BasicWorkflowItemDAOImpl() {
        super();
    }


    @Override
    public BasicWorkflowItem findByItem(Context context, Item i) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BasicWorkflowItem.class);
        Root<BasicWorkflowItem> basicWorkflowItemRoot = criteriaQuery.from(BasicWorkflowItem.class);
        criteriaQuery.select(basicWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(basicWorkflowItemRoot.get(BasicWorkflowItem_.item), i));
        return uniqueResult(context, criteriaQuery, false, BasicWorkflowItem.class, -1, -1);
    }

    @Override
    public List<BasicWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BasicWorkflowItem.class);
        Root<BasicWorkflowItem> basicWorkflowItemRoot = criteriaQuery.from(BasicWorkflowItem.class);
        Join<BasicWorkflowItem, Item> join = basicWorkflowItemRoot.join("item");
        criteriaQuery.select(basicWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(Item_.submitter), ep));

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.asc(basicWorkflowItemRoot.get(BasicWorkflowItem_.workflowitemId)));
        criteriaQuery.orderBy(orderList);


        return list(context, criteriaQuery, false, BasicWorkflowItem.class, -1, -1);

    }

    @Override
    public List<BasicWorkflowItem> findByCollection(Context context, Collection c) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BasicWorkflowItem.class);
        Root<BasicWorkflowItem> basicWorkflowItemRoot = criteriaQuery.from(BasicWorkflowItem.class);
        criteriaQuery.select(basicWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(basicWorkflowItemRoot.get(BasicWorkflowItem_.collection), c));
        return list(context, criteriaQuery, false, BasicWorkflowItem.class, -1, -1);
    }

    @Override
    public List<BasicWorkflowItem> findByPooledTasks(Context context, EPerson ePerson) throws SQLException {
        String queryString = "select wf from TaskListItem as tli join tli.workflowItem wf where tli.ePerson = " +
            ":eperson ORDER BY wf.workflowitemId";
        Query query = createQuery(context, queryString);
        query.setParameter("eperson", ePerson);
        return list(query);
    }

    @Override
    public List<BasicWorkflowItem> findByOwner(Context context, EPerson ePerson) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, BasicWorkflowItem.class);
        Root<BasicWorkflowItem> basicWorkflowItemRoot = criteriaQuery.from(BasicWorkflowItem.class);
        criteriaQuery.select(basicWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(basicWorkflowItemRoot.get(BasicWorkflowItem_.owner), ePerson));
        return list(context, criteriaQuery, false, BasicWorkflowItem.class, -1, -1);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM BasicWorkflowItem"));
    }
}
