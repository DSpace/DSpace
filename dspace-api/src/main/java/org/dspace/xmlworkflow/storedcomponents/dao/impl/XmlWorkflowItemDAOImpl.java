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
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem_;
import org.dspace.xmlworkflow.storedcomponents.dao.XmlWorkflowItemDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the XmlWorkflowItem object.
 * This class is responsible for all database calls for the XmlWorkflowItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class XmlWorkflowItemDAOImpl extends AbstractHibernateDAO<XmlWorkflowItem> implements XmlWorkflowItemDAO {

    protected XmlWorkflowItemDAOImpl() {
        super();
    }

    @Override
    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer offset,
                                                     Integer limit,
                                                     Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, XmlWorkflowItem.class);
        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        criteriaQuery.select(xmlWorkflowItemRoot);
        if (collection != null) {
            criteriaQuery.where(criteriaBuilder.equal(xmlWorkflowItemRoot.get(XmlWorkflowItem_.collection),
                                                      collection));
        }
        if (offset == null) {
            offset = -1;
        }
        if (limit == null) {
            limit = -1;
        }
        criteriaQuery.orderBy(criteriaBuilder.asc(xmlWorkflowItemRoot.get(XmlWorkflowItem_.id)));
        return list(context, criteriaQuery, false, XmlWorkflowItem.class, limit, offset);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return countAllInCollection(context, null);
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {


        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        if (collection != null) {
            criteriaQuery.where(criteriaBuilder.equal(xmlWorkflowItemRoot.get(XmlWorkflowItem_.collection),
                                                      collection));
        }
        return count(context, criteriaQuery, criteriaBuilder, xmlWorkflowItemRoot);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        return findBySubmitter(context, ep, null, null);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep, Integer offset, Integer limit)
            throws SQLException {
        if (offset == null) {
            offset = -1;
        }
        if (limit == null) {
            limit = -1;
        }

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, XmlWorkflowItem.class);
        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        Join<XmlWorkflowItem, Item> join = xmlWorkflowItemRoot.join("item");
        criteriaQuery.select(xmlWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(Item_.submitter), ep));
        criteriaQuery.orderBy(criteriaBuilder.asc(xmlWorkflowItemRoot.get(XmlWorkflowItem_.id)));
        return list(context, criteriaQuery, false, XmlWorkflowItem.class, limit, offset);
    }

    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        Join<XmlWorkflowItem, Item> join = xmlWorkflowItemRoot.join("item");
        criteriaQuery.where(criteriaBuilder.equal(join.get(Item_.submitter), ep));
        return count(context, criteriaQuery, criteriaBuilder, xmlWorkflowItemRoot);
    }

    @Override
    public List<XmlWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, XmlWorkflowItem.class);
        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        criteriaQuery.select(xmlWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(xmlWorkflowItemRoot.get(XmlWorkflowItem_.collection), collection));
        criteriaQuery.orderBy(criteriaBuilder.asc(xmlWorkflowItemRoot.get(XmlWorkflowItem_.id)));
        return list(context, criteriaQuery, false, XmlWorkflowItem.class, -1, -1);
    }

    @Override
    public XmlWorkflowItem findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, XmlWorkflowItem.class);
        Root<XmlWorkflowItem> xmlWorkflowItemRoot = criteriaQuery.from(XmlWorkflowItem.class);
        criteriaQuery.select(xmlWorkflowItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(xmlWorkflowItemRoot.get(XmlWorkflowItem_.item), item));
        return uniqueResult(context, criteriaQuery, false, XmlWorkflowItem.class, -1, -1);
    }
}
