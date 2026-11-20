/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao.impl;

import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBox_;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutField_;
import org.dspace.layout.dao.DynamicLayoutFieldDAO;

/**
 * Database Access Object implementation class for the DynamicLayoutField object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutFieldDAOImpl extends AbstractHibernateDAO<DynamicLayoutField>
        implements DynamicLayoutFieldDAO {

    @Override
    public List<DynamicLayoutField> findByBoxId(Context context, Integer boxId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<DynamicLayoutField> q = cb.createQuery(DynamicLayoutField.class);
        Root<DynamicLayoutBox> boxRoot = q.from(DynamicLayoutBox.class);
        q.where(cb.equal(boxRoot.get(DynamicLayoutBox_.id), boxId));
        ListJoin<DynamicLayoutBox, DynamicLayoutField> join = boxRoot.join(DynamicLayoutBox_.layoutFields);
        CriteriaQuery<DynamicLayoutField> cqFields =
            q.select(join).orderBy(cb.asc(join.get(DynamicLayoutField_.PRIORITY)));
        TypedQuery<DynamicLayoutField> query = getHibernateSession(context).createQuery(cqFields);
        // If present set pagination
        if ( limit != null && offset != null ) {
            query.setFirstResult(offset).setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public Long countByBoxId(Context context, Integer boxId) throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<DynamicLayoutBox> boxRoot = q.from(DynamicLayoutBox.class);
        q.where(cb.equal(boxRoot.get(DynamicLayoutBox_.id), boxId));
        ListJoin<DynamicLayoutBox, DynamicLayoutField> join = boxRoot.join(DynamicLayoutBox_.layoutFields);
        CriteriaQuery<Long> cqFields = q.select(cb.count(join));
        return getHibernateSession(context).createQuery(cqFields).getSingleResult();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.DynamicLayoutFieldDAO#
     * findByBoxId(org.dspace.core.Context, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<DynamicLayoutField> findByBoxId(Context context, Integer boxId, Integer row) throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<DynamicLayoutField> q = cb.createQuery(DynamicLayoutField.class);
        Root<DynamicLayoutBox> boxRoot = q.from(DynamicLayoutBox.class);

        ListJoin<DynamicLayoutBox, DynamicLayoutField> join = boxRoot.join(DynamicLayoutBox_.layoutFields);
        CriteriaQuery<DynamicLayoutField> cqFields = q.select(join)
                .where(
                        cb.equal(join.get(DynamicLayoutField_.ROW), row),
                        cb.equal(boxRoot.get(DynamicLayoutBox_.ID), boxId))
                .orderBy(cb.asc(join.get(DynamicLayoutField_.PRIORITY)));
        TypedQuery<DynamicLayoutField> query = getHibernateSession(context).createQuery(cqFields);
        return query.getResultList();
    }

}
