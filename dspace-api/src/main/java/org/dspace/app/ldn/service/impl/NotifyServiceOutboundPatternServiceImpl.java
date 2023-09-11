/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service.impl;

import java.sql.SQLException;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceOutboundPattern;
import org.dspace.app.ldn.dao.NotifyServiceOutboundPatternDao;
import org.dspace.app.ldn.service.NotifyServiceOutboundPatternService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation Service class for the {@link NotifyServiceOutboundPatternService}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceOutboundPatternServiceImpl implements NotifyServiceOutboundPatternService {

    @Autowired
    private NotifyServiceOutboundPatternDao outboundPatternDao;

    @Override
    public NotifyServiceOutboundPattern findByServiceAndPattern(Context context,
                                                                NotifyServiceEntity notifyServiceEntity,
                                                                String pattern) throws SQLException {
        return outboundPatternDao.findByServiceAndPattern(context, notifyServiceEntity, pattern);
    }

    @Override
    public NotifyServiceOutboundPattern create(Context context, NotifyServiceEntity notifyServiceEntity)
        throws SQLException {
        NotifyServiceOutboundPattern outboundPattern = new NotifyServiceOutboundPattern();
        outboundPattern.setNotifyService(notifyServiceEntity);
        return outboundPatternDao.create(context, outboundPattern);
    }

    @Override
    public void update(Context context, NotifyServiceOutboundPattern outboundPattern) throws SQLException {
        outboundPatternDao.save(context, outboundPattern);
    }

    @Override
    public void delete(Context context, NotifyServiceOutboundPattern outboundPattern) throws SQLException {
        outboundPatternDao.delete(context, outboundPattern);
    }
}
