/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.model.NotifyRequestStatus;
import org.dspace.app.ldn.model.Service;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the {@link LDNMessageEntity} object.
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
    public LDNMessageEntity find(Context context, String id) throws SQLException;

    /**
     * find all ldn messages
     *
     * @param context the context
     * @return all ldn messages by id
     * @throws SQLException If something goes wrong in the database
     */
    public List<LDNMessageEntity> findAll(Context context) throws SQLException;

    /**
     * Creates a new LDNMessage
     *
     * @param context The DSpace context
     * @param id the uri
     * @return the created LDN Message
     * @throws SQLException If something goes wrong in the database
     */
    public LDNMessageEntity create(Context context, String id) throws SQLException;

    /**
     * Creates a new LDNMessage
     *
     * @param context The DSpace context
     * @param notification the requested notification
     * @param sourceIp the source ip
     * @return the created LDN Message
     * @throws SQLException If something goes wrong in the database
     */
    public LDNMessageEntity create(Context context, Notification notification, String sourceIp) throws SQLException;

    /**
     * Update the provided LDNMessage
     *
     * @param context The DSpace context
     * @param ldnMessage the LDNMessage
     * @throws SQLException If something goes wrong in the database
     */
    public void update(Context context, LDNMessageEntity ldnMessage) throws SQLException;

    /**
     * Find the oldest queued LDNMessages that still can be elaborated
     *
     * @return list of LDN messages
     * @param context The DSpace context
     * @throws SQLException If something goes wrong in the database
     */
    public List<LDNMessageEntity> findOldestMessagesToProcess(Context context) throws SQLException;

    /**
     * Find all messages in the queue with the Processing status but timed-out
     * 
     * @return all the LDN Messages to be fixed on their queue_ attributes
     * @param context The DSpace context
     * @throws SQLException If something goes wrong in the database
     */
    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context) throws SQLException;

    /**
     * Find all messages in the queue with the Processing status but timed-out and modify their queue_status
     * considering the queue_attempts
     * 
     * @return number of messages fixed
     * @param context The DSpace context
     * @throws SQLException
     */
    public int checkQueueMessageTimeout(Context context) throws SQLException;

    /**
     * Elaborates the oldest enqueued message
     * 
     * @return number of messages fixed
     * @param context The DSpace context
     */
    public int extractAndProcessMessageFromQueue(Context context) throws SQLException;

    /**
     * find the related notify service entity
     *
     * @param context the context
     * @param service the service
     * @return the NotifyServiceEntity
     * @throws SQLException if something goes wrong
     */
    public NotifyServiceEntity findNotifyService(Context context, Service service) throws SQLException;

    /**
     * find the ldn messages of Requests by item uuid
     *
     * @param context the context
     * @param item the item
     * @return the item requests object
     * @throws SQLException If something goes wrong in the database
     */
    public NotifyRequestStatus findRequestsByItem(Context context, Item item) throws SQLException;

    /**
     * delete the provided ldn message
     *
     * @param context the context
     * @param ldnMessage the ldn message
     * @throws SQLException if something goes wrong
     */
    public void delete(Context context, LDNMessageEntity ldnMessage) throws SQLException;

    /**
     * find the ldn messages to be reprocessed
     *
     * @param context the context
     * @throws SQLException if something goes wrong
     */
    public List<LDNMessageEntity> findMessagesToBeReprocessed(Context context) throws SQLException;

    /**
     * check if IP number is included in the configured ip-range on the Notify
     * Service
     * 
     * @param origin   the Notify Service entity
     * @param sourceIp the ip to evaluate
     */
    public boolean isValidIp(NotifyServiceEntity origin, String sourceIp);

    /**
     * check if the notification is targeting the current system
     * 
     * @param notification   the LDN Message entity
     */
    boolean isTargetCurrent(Notification notification);
}
