/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.notifyservices;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.notifyservices.dao.NotifyServiceOutboundPatternDao;
import org.dspace.notifyservices.service.NotifyServiceOutboundPatternService;
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
}
