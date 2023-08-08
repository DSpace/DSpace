/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.notifyservices.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.notifyservices.NotifyServiceEntity;
import org.dspace.notifyservices.NotifyServiceEntity_;
import org.dspace.notifyservices.NotifyServiceInboundPattern;
import org.dspace.notifyservices.NotifyServiceInboundPattern_;
import org.dspace.notifyservices.dao.NotifyServiceDao;

/**
 * Implementation of {@link NotifyServiceDao}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceDaoImpl extends AbstractHibernateDAO<NotifyServiceEntity> implements NotifyServiceDao {

    @Override
    public List<NotifyServiceEntity> findByLdnUrl(Context context, String ldnUrl) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceEntity.class);
        Root<NotifyServiceEntity> notifyServiceEntityRoot = criteriaQuery.from(NotifyServiceEntity.class);
        criteriaQuery.select(notifyServiceEntityRoot);
        criteriaQuery.where(criteriaBuilder.equal(
            notifyServiceEntityRoot.get(NotifyServiceEntity_.ldnUrl), ldnUrl));
        return list(context, criteriaQuery, false, NotifyServiceEntity.class, -1, -1);
    }

    @Override
    public List<NotifyServiceEntity> findByPattern(Context context, String pattern) throws SQLException {
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
