/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao;

import java.sql.SQLException;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.NotifyServiceOutboundPattern;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * This is the Data Access Object for the {@link NotifyServiceOutboundPattern} object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyServiceOutboundPatternDao extends GenericDAO<NotifyServiceOutboundPattern> {

    /**
     * find all notifyServiceOutboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     *
     * @param context the context
     * @param notifyServiceEntity the notifyServiceEntity
     * @param pattern the pattern
     * @return all notifyServiceInboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     * @throws SQLException if database error
     */
    public NotifyServiceOutboundPattern findByServiceAndPattern(Context context,
                                                               NotifyServiceEntity notifyServiceEntity,
                                                               String pattern) throws SQLException;
}
