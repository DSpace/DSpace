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

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.dao.NotifyPatternToTriggerDao;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link NotifyPatternToTriggerService}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyPatternToTriggerImpl implements NotifyPatternToTriggerService {

    @Autowired(required = true)
    private NotifyPatternToTriggerDao notifyPatternToTriggerDao;

    @Override
    public List<NotifyPatternToTrigger> findAll(Context context) throws SQLException {
        return notifyPatternToTriggerDao.findAll(context, NotifyPatternToTrigger.class);
    }

    @Override
    public List<NotifyPatternToTrigger> findByItem(Context context, Item item) throws SQLException {
        return notifyPatternToTriggerDao.findByItem(context, item);
    }

    @Override
    public List<NotifyPatternToTrigger> findByItemAndPattern(Context context, Item item, String pattern)
        throws SQLException {
        return notifyPatternToTriggerDao.findByItemAndPattern(context, item, pattern);
    }

    @Override
    public NotifyPatternToTrigger create(Context context) throws SQLException {
        NotifyPatternToTrigger notifyPatternToTrigger = new NotifyPatternToTrigger();
        return notifyPatternToTriggerDao.create(context, notifyPatternToTrigger);
    }

    @Override
    public void update(Context context, NotifyPatternToTrigger notifyPatternToTrigger) throws SQLException {
        notifyPatternToTriggerDao.save(context, notifyPatternToTrigger);
    }

    @Override
    public void delete(Context context, NotifyPatternToTrigger notifyPatternToTrigger) throws SQLException {
        notifyPatternToTriggerDao.delete(context, notifyPatternToTrigger);
    }

}
