/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.notifyservices.dao.impl;

import java.sql.SQLException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.notifyservices.NotifyServiceEntity;
import org.dspace.notifyservices.NotifyServiceInboundPattern;
import org.dspace.notifyservices.NotifyServiceInboundPattern_;
import org.dspace.notifyservices.dao.NotifyServiceInboundPatternDao;

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
}
