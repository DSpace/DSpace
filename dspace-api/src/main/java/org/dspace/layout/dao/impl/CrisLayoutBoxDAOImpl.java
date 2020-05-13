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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBox_;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisLayoutTab_;
import org.dspace.layout.dao.CrisLayoutBoxDAO;

public class CrisLayoutBoxDAOImpl extends AbstractHibernateDAO<CrisLayoutBox> implements CrisLayoutBoxDAO {

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
        Root<CrisLayoutTab> tabRoot = q.from(CrisLayoutTab.class);
        q.where(cb.equal(tabRoot.get(CrisLayoutTab_.id), tabId));
        SetJoin<CrisLayoutTab, CrisLayoutBox> tabs = tabRoot.join(CrisLayoutTab_.boxes);
        CriteriaQuery<CrisLayoutBox> cqBoxes = q.select(tabs).orderBy(cb.asc(tabRoot.get(CrisLayoutBox_.PRIORITY)));
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
        Root<CrisLayoutTab> tabRoot = q.from(CrisLayoutTab.class);
        q.where(cb.equal(tabRoot.get(CrisLayoutTab_.id), tabId));
        SetJoin<CrisLayoutTab, CrisLayoutBox> tabs = tabRoot.join(CrisLayoutTab_.boxes);
        CriteriaQuery<Long> cqBoxes = q.select(cb.count(tabs));
        return getHibernateSession(context).createQuery(cqBoxes).getSingleResult();
    }

}
