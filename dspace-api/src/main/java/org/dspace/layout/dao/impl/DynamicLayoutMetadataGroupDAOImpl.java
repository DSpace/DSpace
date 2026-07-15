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
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutField_;
import org.dspace.layout.DynamicMetadataGroup;
import org.dspace.layout.DynamicMetadataGroup_;
import org.dspace.layout.dao.DynamicLayoutMetadataGroupDAO;

/**
 * Database Access Object implementation class for the DynamicLayoutMetadataGroup object
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public class DynamicLayoutMetadataGroupDAOImpl
    extends AbstractHibernateDAO<DynamicMetadataGroup> implements DynamicLayoutMetadataGroupDAO {

    @Override
    public List<DynamicMetadataGroup> findByFieldId(Context context, Integer fieldId,
                                                 @Nullable  Integer limit,
                                                 @Nullable  Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<DynamicMetadataGroup> query = cb.createQuery(DynamicMetadataGroup.class);
        Root<DynamicLayoutField> fieldRoot = query.from(DynamicLayoutField.class);
        query.where(cb.equal(fieldRoot.get(DynamicLayoutField_.id), fieldId));
        ListJoin<DynamicLayoutField, DynamicMetadataGroup> join =
            fieldRoot.join(DynamicLayoutField_.dynamicMetadataGroupList);
        CriteriaQuery<DynamicMetadataGroup> nestedFields =
                query.select(join).orderBy(cb.asc(join.get(DynamicMetadataGroup_.PRIORITY)));
        TypedQuery<DynamicMetadataGroup> queryf = getHibernateSession(context).createQuery(nestedFields);
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
        Root<DynamicLayoutField> fieldRoot = q.from(DynamicLayoutField.class);
        q.where(cb.equal(fieldRoot.get(DynamicLayoutField_.id), field_id));
        ListJoin<DynamicLayoutField, DynamicMetadataGroup> join =
            fieldRoot.join(DynamicLayoutField_.dynamicMetadataGroupList);
        CriteriaQuery<Long> numberOfNestedField = q.select(cb.count(join));
        return getHibernateSession(context).createQuery(numberOfNestedField).getSingleResult();
    }
}
