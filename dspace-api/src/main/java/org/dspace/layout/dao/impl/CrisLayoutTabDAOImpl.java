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
import java.util.UUID;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.dspace.content.EntityType;
import org.dspace.content.EntityType_;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab_;
import org.dspace.layout.dao.CrisLayoutTabDAO;

/**
 * Database Access Object implementation class for the CrisLayoutTab object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutTabDAOImpl extends AbstractHibernateDAO<CrisLayoutTab> implements CrisLayoutTabDAO {

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutTabDAO#countTotal(org.dspace.core.Context)
     */
    @Override
    public Long countTotal(Context context) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);
        cc.select(cb.count(cc.from(CrisLayoutTab.class)));
        return getHibernateSession(context).createQuery(cc).getSingleResult();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutTabDAO#findByEntityType(java.lang.String)
     */
    @Override
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType) throws SQLException {
        return findByEntityType(context, entityType, null, null);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutTabDAO#findByEntityType
     * (org.dspace.core.Context, java.lang.String, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutTab> findByEntityType(Context context, String entityType, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<CrisLayoutTab> query = cb.createQuery(CrisLayoutTab.class);
        Root<CrisLayoutTab> tabRoot = query.from(CrisLayoutTab.class);
        tabRoot.fetch(CrisLayoutTab_.entity, JoinType.LEFT);
        query
            .where(cb.equal(tabRoot.get(CrisLayoutTab_.entity).get(EntityType_.LABEL), entityType))
            .orderBy(cb.asc(tabRoot.get(CrisLayoutTab_.PRIORITY)));

        TypedQuery<CrisLayoutTab> exQuery = getHibernateSession(context).createQuery(query);
        if ( limit != null && offset != null ) {
            exQuery.setFirstResult(offset).setMaxResults(limit);
        }
        return exQuery.getResultList();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutTabDAO#countByEntityType(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public Long countByEntityType(Context context, String entityType) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);

        Root<CrisLayoutTab> tabRoot = cc.from(CrisLayoutTab.class);
        Join<CrisLayoutTab, EntityType> join = tabRoot.join(CrisLayoutTab_.entity);

        cc.select(cb.count(tabRoot))
            .where(cb.equal(join.get(EntityType_.LABEL), entityType));
        return getHibernateSession(context).createQuery(cc).getSingleResult();
    }

    @Override
    public Long totalMetadatafield(Context context, Integer tabId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> metadatacount = cb.createQuery(Long.class);
        Root<CrisLayoutTab> tabRoot = metadatacount.from(CrisLayoutTab.class);
        Join<CrisLayoutTab, MetadataField> join = tabRoot.join(CrisLayoutTab_.metadataSecurityFields);
        metadatacount.select(cb.count(join)).where(cb.equal(tabRoot.get(CrisLayoutTab_.ID), tabId));
        return getHibernateSession(context).createQuery(metadatacount).getSingleResult();
    }

    @Override
    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<MetadataField> metadata = cb.createQuery(MetadataField.class);
        Root<CrisLayoutTab> tabRoot = metadata.from(CrisLayoutTab.class);
        Join<CrisLayoutTab, MetadataField> join = tabRoot.join(CrisLayoutTab_.metadataSecurityFields);
        metadata.select(join).where(cb.equal(tabRoot.get(CrisLayoutTab_.ID), tabId));
        TypedQuery<MetadataField> query = getHibernateSession(context).createQuery(metadata);
        if (limit != null && offset != null) {
            query.setMaxResults(limit).setFirstResult(offset);
        }
        return query.getResultList();
    }

    @Override
    public List<CrisLayoutTab> findByItem(Context context, UUID intemUUid) throws SQLException {
        return null;
    }

}
