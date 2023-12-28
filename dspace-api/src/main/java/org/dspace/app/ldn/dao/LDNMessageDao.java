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

    public List<LDNMessageEntity> findOldestMessageToProcess(Context context, int max_attempts) throws SQLException;

    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context, int max_attempts) throws SQLException;

    public List<LDNMessageEntity> findAllMessagesByItem(
        Context context, Item item, String... activities) throws SQLException;

    public List<LDNMessageEntity> findAllRelatedMessagesByItem(
        Context context, LDNMessageEntity msg, Item item, String... relatedTypes) throws SQLException;

    public List<LDNMessageEntity> findMessagesToBeReprocessed(Context context) throws SQLException;
}
