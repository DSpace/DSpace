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

import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the LDNMessage object.
 *
 * The implementation of this class is responsible for all database calls for
 * the LDNMessage object and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface LDNMessageDao extends GenericDAO<LDNMessageEntity> {

    /**
     * load the oldest ldn messages considering their {@link org.dspace.app.ldn.LDNMessageEntity#queueLastStartTime}
     * @param context
     * @param max_attempts consider ldn_message entity with queue_attempts <= max_attempts
     * @return ldn message entities to be routed
     * @throws SQLException
     */
    public List<LDNMessageEntity> findOldestMessageToProcess(Context context, int max_attempts) throws SQLException;

    /**
     * find ldn message entties in processing status and already timed out.
     * @param context
     * @param max_attempts consider ldn_message entity with queue_attempts <= max_attempts
     * @return ldn message entities
     * @throws SQLException
     */
    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context, int max_attempts) throws SQLException;

    /**
     * find all ldn messages related to an item
     * @param context
     * @param item item related to the returned ldn messages
     * @param activities involves only this specific group of activities
     * @return all ldn messages related to the given item
     * @throws SQLException
     */
    public List<LDNMessageEntity> findAllMessagesByItem(
        Context context, Item item, String... activities) throws SQLException;

    /**
     * find all ldn messages related to an item and to a specific ldn message
     * @param context
     * @param msg the referring ldn message
     * @param item the referring repository item
     * @param relatedTypes filter for @see org.dspace.app.ldn.LDNMessageEntity#activityStreamType
     * @return all related ldn messages
     * @throws SQLException
     */
    public List<LDNMessageEntity> findAllRelatedMessagesByItem(
        Context context, LDNMessageEntity msg, Item item, String... relatedTypes) throws SQLException;

    /**
     *
     * @param context
     * @return the list of messages in need to be reprocessed - with queue_status as QUEUE_STATUS_QUEUED_FOR_RETRY
     * @throws SQLException
     */
    public List<LDNMessageEntity> findMessagesToBeReprocessed(Context context) throws SQLException;
}
