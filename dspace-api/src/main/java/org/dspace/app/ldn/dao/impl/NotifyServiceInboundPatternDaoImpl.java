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
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.NotifyServiceInboundPattern_;
import org.dspace.app.ldn.dao.NotifyServiceInboundPatternDao;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link NotifyServiceInboundPatternDao}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceInboundPatternDaoImpl
    extends AbstractHibernateDAO<NotifyServiceInboundPattern> implements NotifyServiceInboundPatternDao {

    @Override
    public NotifyServiceInboundPattern findByServiceAndPattern(Context context, NotifyServiceEntity notifyServiceEntity,
                                                               String pattern) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceInboundPattern.class);
        Root<NotifyServiceInboundPattern> inboundPatternRoot = criteriaQuery.from(NotifyServiceInboundPattern.class);
        criteriaQuery.select(inboundPatternRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(
                inboundPatternRoot.get(NotifyServiceInboundPattern_.notifyService), notifyServiceEntity),
            criteriaBuilder.equal(
                inboundPatternRoot.get(NotifyServiceInboundPattern_.pattern), pattern)
        ));
        return uniqueResult(context, criteriaQuery, false, NotifyServiceInboundPattern.class);
    }

    @Override
    public List<NotifyServiceInboundPattern> findAutomaticPatterns(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceInboundPattern.class);
        Root<NotifyServiceInboundPattern> inboundPatternRoot = criteriaQuery.from(NotifyServiceInboundPattern.class);
        criteriaQuery.select(inboundPatternRoot);
        criteriaQuery.where(
            criteriaBuilder.equal(
                inboundPatternRoot.get(NotifyServiceInboundPattern_.automatic), true)
        );
        return list(context, criteriaQuery, false, NotifyServiceInboundPattern.class, -1, -1);
    }
}
