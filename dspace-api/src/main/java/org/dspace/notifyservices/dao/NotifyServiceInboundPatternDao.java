/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.notifyservices.dao;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.notifyservices.NotifyServiceEntity;
import org.dspace.notifyservices.NotifyServiceInboundPattern;

/**
 * This is the Data Access Object for the {@link NotifyServiceInboundPattern} object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyServiceInboundPatternDao extends GenericDAO<NotifyServiceInboundPattern> {

    /**
     * find all notifyServiceInboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     *
     * @param context the context
     * @param notifyServiceEntity the notifyServiceEntity
     * @param pattern the pattern
     * @return all notifyServiceInboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     * @throws SQLException if database error
     */
    public NotifyServiceInboundPattern findByServiceAndPattern(Context context,
                                                               NotifyServiceEntity notifyServiceEntity,
                                                               String pattern) throws SQLException;
}
