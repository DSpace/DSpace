/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import java.sql.SQLException;

import org.dspace.app.ldn.LDNMessage;
import org.dspace.app.ldn.model.Notification;
import org.dspace.core.Context;

/**
 * Service interface class for the {@link LDNMessage} object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface LDNMessageService {

    /**
     * find the ldn message by id
     *
     * @param context the context
     * @param id the uri
     * @return the ldn message by id
     * @throws SQLException If something goes wrong in the database
     */
    public LDNMessage find(Context context, String id) throws SQLException;

    /**
     * Creates a new LDNMessage
     *
     * @param context The DSpace context
     * @param id the uri
     * @return the created LDN Message
     * @throws SQLException If something goes wrong in the database
     */
    public LDNMessage create(Context context, String id) throws SQLException;

    /**
     * Creates a new LDNMessage
     *
     * @param context The DSpace context
     * @param notification the requested notification
     * @return the created LDN Message
     * @throws SQLException If something goes wrong in the database
     */
    public LDNMessage create(Context context, Notification notification) throws SQLException;

    /**
     * Update the provided LDNMessage
     *
     * @param context The DSpace context
     * @param ldnMessage the LDNMessage
     * @throws SQLException If something goes wrong in the database
     */
    public void update(Context context, LDNMessage ldnMessage) throws SQLException;
}
