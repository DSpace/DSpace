/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.notifyservices.service;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.notifyservices.NotifyServiceEntity;
import org.dspace.notifyservices.NotifyServiceOutboundPattern;

/**
 * Service interface class for the {@link NotifyServiceOutboundPattern} object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyServiceOutboundPatternService {

    /**
     * find all notifyServiceOutboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     *
     * @param context the context
     * @param notifyServiceEntity the notifyServiceEntity
     * @param pattern the pattern
     * @return all notifyServiceOutboundPatterns matched with
     * the provided notifyServiceEntity and pattern
     * @throws SQLException if database error
     */
    public NotifyServiceOutboundPattern findByServiceAndPattern(Context context,
                                                                NotifyServiceEntity notifyServiceEntity,
                                                                String pattern) throws SQLException;

    /**
     * create new notifyServiceOutboundPattern
     *
     * @param context the context
     * @param notifyServiceEntity the notifyServiceEntity
     * @return the created notifyServiceOutboundPattern
     * @throws SQLException if database error
     */
    public NotifyServiceOutboundPattern create(Context context, NotifyServiceEntity notifyServiceEntity)
        throws SQLException;

    /**
     * update the provided notifyServiceOutboundPattern
     *
     * @param context the context
     * @param outboundPattern the notifyServiceOutboundPattern
     * @throws SQLException if database error
     */
    public void update(Context context, NotifyServiceOutboundPattern outboundPattern) throws SQLException;
}
