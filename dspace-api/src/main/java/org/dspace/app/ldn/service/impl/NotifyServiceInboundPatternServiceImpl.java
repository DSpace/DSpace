/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.ldn.dao.NotifyServiceInboundPatternDao;
import org.dspace.app.ldn.service.NotifyServiceInboundPatternService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation Service class for the {@link NotifyServiceInboundPatternService}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceInboundPatternServiceImpl implements NotifyServiceInboundPatternService {

    @Autowired
    private NotifyServiceInboundPatternDao inboundPatternDao;

    @Override
    public NotifyServiceInboundPattern findByServiceAndPattern(Context context,
                                                               NotifyServiceEntity notifyServiceEntity,
                                                               String pattern) throws SQLException {
        return inboundPatternDao.findByServiceAndPattern(context, notifyServiceEntity, pattern);
    }

    @Override
    public List<NotifyServiceInboundPattern> findAutomaticPatterns(Context context) throws SQLException {
        return inboundPatternDao.findAutomaticPatterns(context);
    }

    @Override
    public NotifyServiceInboundPattern create(Context context, NotifyServiceEntity notifyServiceEntity)
        throws SQLException {
        NotifyServiceInboundPattern inboundPattern = new NotifyServiceInboundPattern();
        inboundPattern.setNotifyService(notifyServiceEntity);
        return inboundPatternDao.create(context, inboundPattern);
    }

    @Override
    public void update(Context context, NotifyServiceInboundPattern inboundPattern) throws SQLException {
        inboundPatternDao.save(context, inboundPattern);
    }

    @Override
    public void delete(Context context, NotifyServiceInboundPattern inboundPattern) throws SQLException {
        inboundPatternDao.delete(context, inboundPattern);
    }
}
