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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceEntity_;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.NotifyServiceInboundPattern_;
import org.dspace.app.ldn.dao.NotifyServiceDao;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link NotifyServiceDao}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceDaoImpl extends AbstractHibernateDAO<NotifyServiceEntity> implements NotifyServiceDao {

    @Override
    public NotifyServiceEntity findByLdnUrl(Context context, String ldnUrl) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceEntity.class);
        Root<NotifyServiceEntity> notifyServiceEntityRoot = criteriaQuery.from(NotifyServiceEntity.class);
        criteriaQuery.select(notifyServiceEntityRoot);
        criteriaQuery.where(criteriaBuilder.equal(
            notifyServiceEntityRoot.get(NotifyServiceEntity_.ldnUrl), ldnUrl));
        return uniqueResult(context, criteriaQuery, false, NotifyServiceEntity.class);
    }

    @Override
    public List<NotifyServiceEntity> findManualServicesByInboundPattern(Context context, String pattern)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceEntity.class);
        Root<NotifyServiceEntity> notifyServiceEntityRoot = criteriaQuery.from(NotifyServiceEntity.class);

        Join<NotifyServiceEntity, NotifyServiceInboundPattern> notifyServiceInboundPatternJoin =
            notifyServiceEntityRoot.join(NotifyServiceEntity_.inboundPatterns);

        criteriaQuery.select(notifyServiceEntityRoot);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(
                    notifyServiceInboundPatternJoin.get(NotifyServiceInboundPattern_.pattern), pattern),
                criteriaBuilder.equal(
                    notifyServiceInboundPatternJoin.get(NotifyServiceInboundPattern_.automatic), false)));

        return list(context, criteriaQuery, false, NotifyServiceEntity.class, -1, -1);
    }
}
