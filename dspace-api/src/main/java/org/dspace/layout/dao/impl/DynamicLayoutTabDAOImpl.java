/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao.impl;

import static org.dspace.layout.DynamicLayoutTab.ROWS_AND_CONTENT_GRAPH;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.EntityType;
import org.dspace.content.EntityType_;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutTab;
import org.dspace.layout.DynamicLayoutTab_;
import org.dspace.layout.dao.DynamicLayoutTabDAO;

/**
 * Database Access Object implementation class for the DynamicLayoutTab object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutTabDAOImpl extends AbstractHibernateDAO<DynamicLayoutTab> implements DynamicLayoutTabDAO {

    @Override
    public DynamicLayoutTab findAndEagerlyFetchBoxes(Context context, Integer id) throws SQLException {

        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<DynamicLayoutTab> query = cb.createQuery(DynamicLayoutTab.class);
        Root<DynamicLayoutTab> tabRoot = query.from(DynamicLayoutTab.class);

        query.where(cb.equal(tabRoot.get(DynamicLayoutTab_.ID), id));

        TypedQuery<DynamicLayoutTab> typedQuery = getHibernateSession(context).createQuery(query);
        EntityGraph<?> graph = getHibernateSession(context).createEntityGraph(ROWS_AND_CONTENT_GRAPH);
        typedQuery.setHint("javax.persistence.loadgraph", graph);

        return singleResult(typedQuery);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.DynamicLayoutTabDAO#countTotal(org.dspace.core.Context)
     */
    @Override
    public Long countTotal(Context context) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);
        cc.select(cb.count(cc.from(DynamicLayoutTab.class)));
        return getHibernateSession(context).createQuery(cc).getSingleResult();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.DynamicLayoutTabDAO#findByEntityType(java.lang.String)
     */
    @Override
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(Context context,
        String entityType, String customFilter) throws SQLException {
        return findByEntityTypeAndEagerlyFetchBoxes(context, entityType, customFilter, null, null);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.DynamicLayoutTabDAO#findByEntityType(java.lang.String)
     */
    @Override
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(Context context,
        String entityType) throws SQLException {
        return findByEntityTypeAndEagerlyFetchBoxes(context, entityType, null, null, null);
    }

    @Override
    public List<DynamicLayoutTab> findByEntityTypeAndEagerlyFetchBoxes(Context context, String entityType,
        String customFilter, Integer limit, Integer offset) throws SQLException {

        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<DynamicLayoutTab> query = cb.createQuery(DynamicLayoutTab.class);
        Root<DynamicLayoutTab> tabRoot = query.from(DynamicLayoutTab.class);
        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(tabRoot.get(DynamicLayoutTab_.entity).get(EntityType_.LABEL), entityType));
        if (StringUtils.isNotBlank(customFilter)) {
            andPredicates.add(cb.equal(tabRoot.get(DynamicLayoutTab_.CUSTOM_FILTER), customFilter));
        } else {
            andPredicates.add(cb.isNull((tabRoot.get(DynamicLayoutTab_.CUSTOM_FILTER))));
        }

        query
            .where(andPredicates.toArray(new Predicate[] {}))
            .orderBy(cb.asc(tabRoot.get(DynamicLayoutTab_.PRIORITY)));

        TypedQuery<DynamicLayoutTab> typedQuery = getHibernateSession(context).createQuery(query);
        if ( limit != null && offset != null ) {
            typedQuery.setFirstResult(offset).setMaxResults(limit);
        }

        EntityGraph<?> graph = getHibernateSession(context).createEntityGraph(ROWS_AND_CONTENT_GRAPH);
        typedQuery.setHint("javax.persistence.loadgraph", graph);

        return typedQuery.getResultList();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.DynamicLayoutTabDAO#countByEntityType(org.dspace.core.Context, java.lang.String)
     */
    @Override
    public Long countByEntityType(Context context, String entityType) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);

        Root<DynamicLayoutTab> tabRoot = cc.from(DynamicLayoutTab.class);
        Join<DynamicLayoutTab, EntityType> join = tabRoot.join(DynamicLayoutTab_.entity);

        cc.select(cb.count(tabRoot))
            .where(cb.equal(join.get(EntityType_.LABEL), entityType));
        return getHibernateSession(context).createQuery(cc).getSingleResult();
    }

    @Override
    public Long totalMetadatafield(Context context, Integer tabId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> metadatacount = cb.createQuery(Long.class);
        Root<DynamicLayoutTab> tabRoot = metadatacount.from(DynamicLayoutTab.class);
        Join<DynamicLayoutTab, MetadataField> join = tabRoot.join(DynamicLayoutTab_.metadataSecurityFields);
        metadatacount.select(cb.count(join)).where(cb.equal(tabRoot.get(DynamicLayoutTab_.ID), tabId));
        return getHibernateSession(context).createQuery(metadatacount).getSingleResult();
    }

    @Override
    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<MetadataField> metadata = cb.createQuery(MetadataField.class);
        Root<DynamicLayoutTab> tabRoot = metadata.from(DynamicLayoutTab.class);
        Join<DynamicLayoutTab, MetadataField> join = tabRoot.join(DynamicLayoutTab_.metadataSecurityFields);
        metadata.select(join).where(cb.equal(tabRoot.get(DynamicLayoutTab_.ID), tabId));
        TypedQuery<MetadataField> query = getHibernateSession(context).createQuery(metadata);
        if (limit != null && offset != null) {
            query.setMaxResults(limit).setFirstResult(offset);
        }
        return query.getResultList();
    }

}
