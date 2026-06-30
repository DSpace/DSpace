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

import jakarta.annotation.Nullable;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutField_;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.CrisMetadataGroup_;
import org.dspace.layout.dao.CrisLayoutMetadataGroupDAO;

/**
 * Database Access Object implementation class for the CrisLayoutMetadataGroup object
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public class CrisLayoutMetadataGroupDAOImpl
    extends AbstractHibernateDAO<CrisMetadataGroup> implements CrisLayoutMetadataGroupDAO {

    @Override
    public List<CrisMetadataGroup> findByFieldId(Context context, Integer fieldId,
                                                 @Nullable  Integer limit,
                                                 @Nullable  Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<CrisMetadataGroup> query = cb.createQuery(CrisMetadataGroup.class);
        Root<CrisLayoutField> fieldRoot = query.from(CrisLayoutField.class);
        query.where(cb.equal(fieldRoot.get(CrisLayoutField_.id), fieldId));
        ListJoin<CrisLayoutField, CrisMetadataGroup> join = fieldRoot.join(CrisLayoutField_.crisMetadataGroupList);
        CriteriaQuery<CrisMetadataGroup> nestedFields =
                query.select(join).orderBy(cb.asc(join.get(CrisMetadataGroup_.PRIORITY)));
        TypedQuery<CrisMetadataGroup> queryf = getHibernateSession(context).createQuery(nestedFields);
        // If present set pagination
        if (limit != null && offset != null) {
            queryf.setFirstResult(offset).setMaxResults(limit);
        }
        return queryf.getResultList();
    }
    @Override
    public Long countByFieldId(Context context, Integer field_id) throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<CrisLayoutField> fieldRoot = q.from(CrisLayoutField.class);
        q.where(cb.equal(fieldRoot.get(CrisLayoutField_.id), field_id));
        ListJoin<CrisLayoutField, CrisMetadataGroup> join = fieldRoot.join(CrisLayoutField_.crisMetadataGroupList);
        CriteriaQuery<Long> numberOfNestedField = q.select(cb.count(join));
        return getHibernateSession(context).createQuery(numberOfNestedField).getSingleResult();
    }
}
