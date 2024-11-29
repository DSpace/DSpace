/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao.impl;

import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyPatternToTrigger_;
import org.dspace.app.ldn.dao.NotifyPatternToTriggerDao;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link NotifyPatternToTriggerDao}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyPatternToTriggerDaoImpl extends AbstractHibernateDAO<NotifyPatternToTrigger>
    implements NotifyPatternToTriggerDao {

    @Override
    public List<NotifyPatternToTrigger> findByItem(Context context, Item item)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyPatternToTrigger.class);
        Root<NotifyPatternToTrigger> notifyServiceEntityRoot = criteriaQuery.from(NotifyPatternToTrigger.class);
        criteriaQuery.select(notifyServiceEntityRoot);
        criteriaQuery.where(criteriaBuilder.equal(
            notifyServiceEntityRoot.get(NotifyPatternToTrigger_.item), item));
        return list(context, criteriaQuery, false, NotifyPatternToTrigger.class, -1, -1);
    }
    @Override
    public List<NotifyPatternToTrigger> findByItemAndPattern(Context context, Item item, String pattern)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyPatternToTrigger.class);
        Root<NotifyPatternToTrigger> notifyServiceEntityRoot = criteriaQuery.from(NotifyPatternToTrigger.class);
        criteriaQuery.select(notifyServiceEntityRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(
            notifyServiceEntityRoot.get(NotifyPatternToTrigger_.item), item),
            criteriaBuilder.equal(
                notifyServiceEntityRoot.get(NotifyPatternToTrigger_.pattern), pattern)
            ));
        return list(context, criteriaQuery, false, NotifyPatternToTrigger.class, -1, -1);
    }

}
