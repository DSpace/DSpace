/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * This is the Data Access Object for the {@link NotifyServiceEntity} object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyServiceDao extends GenericDAO<NotifyServiceEntity> {
    /**
     * find the NotifyServiceEntity matched with the provided ldnUrl
     *
     * @param context the context
     * @param ldnUrl the ldnUrl
     * @return the NotifyServiceEntity matched the provided ldnUrl
     * @throws SQLException if database error
     */
    public NotifyServiceEntity findByLdnUrl(Context context, String ldnUrl) throws SQLException;

    /**
     * find all NotifyServiceEntity matched the provided inbound pattern
     * from the related notifyServiceInboundPatterns
     * also with 'automatic' equals to false
     *
     * @param context the context
     * @param pattern the ldnUrl
     * @return all NotifyServiceEntity matched the provided pattern
     * @throws SQLException if database error
     */
    public List<NotifyServiceEntity> findManualServicesByInboundPattern(Context context, String pattern)
        throws SQLException;
}
