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
import org.dspace.notifyservices.NotifyServiceOutboundPattern;
import org.dspace.notifyservices.NotifyServiceOutboundPattern_;
import org.dspace.notifyservices.dao.NotifyServiceOutboundPatternDao;

/**
 * Implementation of {@link NotifyServiceOutboundPatternDao}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceOutboundPatternDaoImpl
    extends AbstractHibernateDAO<NotifyServiceOutboundPattern> implements NotifyServiceOutboundPatternDao {

    @Override
    public NotifyServiceOutboundPattern findByServiceAndPattern(Context context,
                                                                NotifyServiceEntity notifyServiceEntity,
                                                                String pattern) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, NotifyServiceOutboundPattern.class);
        Root<NotifyServiceOutboundPattern> outboundPatternRoot = criteriaQuery.from(NotifyServiceOutboundPattern.class);
        criteriaQuery.select(outboundPatternRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(
                outboundPatternRoot.get(NotifyServiceOutboundPattern_.notifyService), notifyServiceEntity),
            criteriaBuilder.equal(
                outboundPatternRoot.get(NotifyServiceOutboundPattern_.pattern), pattern)
        ));
        return uniqueResult(context, criteriaQuery, false, NotifyServiceOutboundPattern.class);
    }
}
