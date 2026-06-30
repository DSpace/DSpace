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
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox_;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutField_;
import org.dspace.layout.dao.CrisLayoutFieldDAO;

/**
 * Database Access Object implementation class for the CrisLayoutField object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldDAOImpl extends AbstractHibernateDAO<CrisLayoutField> implements CrisLayoutFieldDAO {

    @Override
    public List<CrisLayoutField> findByBoxId(Context context, Integer boxId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<CrisLayoutField> q = cb.createQuery(CrisLayoutField.class);
        Root<CrisLayoutBox> boxRoot = q.from(CrisLayoutBox.class);
        q.where(cb.equal(boxRoot.get(CrisLayoutBox_.id), boxId));
        ListJoin<CrisLayoutBox, CrisLayoutField> join = boxRoot.join(CrisLayoutBox_.layoutFields);
        CriteriaQuery<CrisLayoutField> cqFields = q.select(join).orderBy(cb.asc(join.get(CrisLayoutField_.PRIORITY)));
        TypedQuery<CrisLayoutField> query = getHibernateSession(context).createQuery(cqFields);
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
        Root<CrisLayoutBox> boxRoot = q.from(CrisLayoutBox.class);
        q.where(cb.equal(boxRoot.get(CrisLayoutBox_.id), boxId));
        ListJoin<CrisLayoutBox, CrisLayoutField> join = boxRoot.join(CrisLayoutBox_.layoutFields);
        CriteriaQuery<Long> cqFields = q.select(cb.count(join));
        return getHibernateSession(context).createQuery(cqFields).getSingleResult();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutFieldDAO#
     * findByBoxId(org.dspace.core.Context, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutField> findByBoxId(Context context, Integer boxId, Integer row) throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<CrisLayoutField> q = cb.createQuery(CrisLayoutField.class);
        Root<CrisLayoutBox> boxRoot = q.from(CrisLayoutBox.class);

        ListJoin<CrisLayoutBox, CrisLayoutField> join = boxRoot.join(CrisLayoutBox_.layoutFields);
        CriteriaQuery<CrisLayoutField> cqFields = q.select(join)
                .where(
                        cb.equal(join.get(CrisLayoutField_.ROW), row),
                        cb.equal(boxRoot.get(CrisLayoutBox_.ID), boxId))
                .orderBy(cb.asc(join.get(CrisLayoutField_.PRIORITY)));
        TypedQuery<CrisLayoutField> query = getHibernateSession(context).createQuery(cqFields);
        return query.getResultList();
    }

}
