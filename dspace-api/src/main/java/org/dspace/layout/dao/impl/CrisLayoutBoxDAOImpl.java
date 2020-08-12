/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.EntityType;
import org.dspace.content.EntityType_;
import org.dspace.content.MetadataField;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox_;
import org.dspace.layout.CrisLayoutTab2Box;
import org.dspace.layout.CrisLayoutTab2BoxId_;
import org.dspace.layout.CrisLayoutTab2Box_;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Database Access Object implementation class for the CrisLayoutBox object
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutBoxDAOImpl extends AbstractHibernateDAO<CrisLayoutBox> implements CrisLayoutBoxDAO {

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutBoxDAO#findByTabId(org.dspace.core.Context, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId) throws SQLException {
        return findByTabId(context, tabId, null, null);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutBoxDAO#findByTabId
     * (org.dspace.core.Context, java.lang.Integer, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<CrisLayoutBox> findByTabId(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<CrisLayoutBox> q = cb.createQuery(CrisLayoutBox.class);
        Root<CrisLayoutTab2Box> tab2boxRoot = q.from(CrisLayoutTab2Box.class);
        q.where(cb.equal(tab2boxRoot.get(CrisLayoutTab2Box_.id).get(CrisLayoutTab2BoxId_.CRIS_LAYOUT_TAB_ID), tabId));
        q.orderBy(cb.asc(tab2boxRoot.get(CrisLayoutTab2Box_.POSITION)));
        Join<CrisLayoutTab2Box, CrisLayoutBox> tab2boxes = tab2boxRoot.join(CrisLayoutTab2Box_.BOX);
        CriteriaQuery<CrisLayoutBox> cqBoxes = q.select(tab2boxes);
        TypedQuery<CrisLayoutBox> query = getHibernateSession(context).createQuery(cqBoxes);
        // If present set pagination
        if ( limit != null && offset != null ) {
            query.setFirstResult(offset).setMaxResults(limit);
        }
        return query.getResultList();
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.dao.CrisLayoutBoxDAO#countTotalBoxesInTab
     * (org.dspace.core.Context, java.lang.Integer)
     */
    @Override
    public Long countTotalBoxesInTab(Context context, Integer tabId) throws SQLException {
        CriteriaBuilder cb = getHibernateSession(context).getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<CrisLayoutTab2Box> tab2boxRoot = q.from(CrisLayoutTab2Box.class);
        q.where(cb.equal(tab2boxRoot.get(CrisLayoutTab2Box_.id).get(CrisLayoutTab2BoxId_.CRIS_LAYOUT_TAB_ID), tabId));
        CriteriaQuery<Long> cqBoxes = q.select(cb.count(tab2boxRoot));
        return getHibernateSession(context).createQuery(cqBoxes).getSingleResult();
    }

    @Override
    public Long countTotalEntityBoxes(Context context, String entityType) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> cc = cb.createQuery(Long.class);
        Root<CrisLayoutBox> boxRoot = cc.from(CrisLayoutBox.class);
        Join<CrisLayoutBox, EntityType> join = boxRoot.join(CrisLayoutBox_.entitytype);

        cc.select(cb.count(boxRoot))
            .where(cb.equal(join.get(EntityType_.LABEL), entityType));
        return getHibernateSession(context).createQuery(cc).getSingleResult();
    }

    @Override
    public List<CrisLayoutBox> findByEntityType(
            Context context, String entityType, Integer tabId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<CrisLayoutBox> query = cb.createQuery(CrisLayoutBox.class);
        Root<CrisLayoutBox> boxRoot = query.from(CrisLayoutBox.class);
        boxRoot.fetch(CrisLayoutBox_.entitytype, JoinType.LEFT);
        // Initialize dynamic predicates list
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(boxRoot.get(CrisLayoutBox_.entitytype).get(EntityType_.LABEL), entityType));

        // Set filter if tabId parameter isn't null
        if (tabId != null) {
            Join<CrisLayoutBox, CrisLayoutTab2Box> tabs = boxRoot.join(CrisLayoutBox_.TAB2BOX);
            predicates.add(cb.equal(tabs.get(CrisLayoutTab2Box_.ID)
                    .get(CrisLayoutTab2BoxId_.CRIS_LAYOUT_TAB_ID), tabId));
            query.orderBy(cb.asc(tabs.get(CrisLayoutTab2Box_.POSITION)));
        }

        Predicate[] predicateArray = new Predicate[predicates.size()];
        predicates.toArray(predicateArray);
        // Set where condition and orderBy
        query.select(boxRoot)
            .where(predicateArray);
        TypedQuery<CrisLayoutBox> exQuery = getHibernateSession(context).createQuery(query);
        // If present set pagination filter
        if ( limit != null && offset != null ) {
            exQuery.setFirstResult(offset).setMaxResults(limit);
        }
        return exQuery.getResultList();
    }

    @Override
    public Long totalMetadatafield(Context context, Integer boxId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Long> metadatacount = cb.createQuery(Long.class);
        Root<CrisLayoutBox> tabRoot = metadatacount.from(CrisLayoutBox.class);
        Join<CrisLayoutBox, MetadataField> join = tabRoot.join(CrisLayoutBox_.metadataSecurityFields);
        metadatacount.select(cb.count(join)).where(cb.equal(tabRoot.get(CrisLayoutBox_.ID), boxId));
        return getHibernateSession(context).createQuery(metadatacount).getSingleResult();
    }

    @Override
    public List<MetadataField> getMetadataField(Context context, Integer boxId, Integer limit, Integer offset)
            throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<MetadataField> metadata = cb.createQuery(MetadataField.class);
        Root<CrisLayoutBox> tabRoot = metadata.from(CrisLayoutBox.class);
        Join<CrisLayoutBox, MetadataField> join = tabRoot.join(CrisLayoutBox_.metadataSecurityFields);
        metadata.select(join).where(cb.equal(tabRoot.get(CrisLayoutBox_.ID), boxId));
        TypedQuery<MetadataField> query = getHibernateSession(context).createQuery(metadata);
        if (limit != null && offset != null) {
            query.setMaxResults(limit).setFirstResult(offset);
        }
        return query.getResultList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.layout.dao.CrisLayoutBoxDAO#findByShortname(org.dspace.core.
     * Context, java.lang.String, java.lang.String)
     */
    @Override
    public CrisLayoutBox findByShortname(Context context, Integer entityTypeId, String shortname) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<CrisLayoutBox> query = cb.createQuery(CrisLayoutBox.class);
        Root<CrisLayoutBox> boxRoot = query.from(CrisLayoutBox.class);
        Join<CrisLayoutBox, EntityType> join = boxRoot.join(CrisLayoutBox_.entitytype);
        query.where(
                cb.and(
                        cb.equal(boxRoot.get(CrisLayoutBox_.SHORTNAME), shortname),
                        cb.equal(join.get(EntityType_.id), entityTypeId)));
        TypedQuery<CrisLayoutBox> tq = getHibernateSession(context).createQuery(query);
        return tq.getSingleResult();
    }

}
