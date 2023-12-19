/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.QAEventProcessed;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;

/**
 * DAO that handle processed QA Events.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface QAEventsDAO extends GenericDAO<QAEventProcessed> {

    /**
     * Returns all the stored QAEventProcessed entities.
     *
     * @param  context      the DSpace context
     * @return              the found entities
     * @throws SQLException if an SQL error occurs
     */
    public List<QAEventProcessed> findAll(Context context) throws SQLException;

    /**
     * Returns the stored QAEventProcessed entities by item.
     *
     * @param  context      the DSpace context
     * @param  item         the item to search for
     * @return              the found entities
     * @throws SQLException if an SQL error occurs
     */
    public List<QAEventProcessed> findByItem(Context context, Item item) throws SQLException;

    /**
     * Returns the stored QAEventProcessed entities by eperson.
     *
     * @param  context      the DSpace context
     * @param  ePerson      the ePerson to search for
     * @return              the found entities
     * @throws SQLException if an SQL error occurs
     */
    public List<QAEventProcessed> findByEPerson(Context context, EPerson ePerson) throws SQLException;

    /**
     * Search a page of quality assurance broker events by notification ID.
     * 
     * @param  context      the DSpace context
     * @param  eventId      the event id
     * @param  start        the start index
     * @param  size         the size to be applied
     * @return              the processed events
     * @throws SQLException if an SQL error occurs
     */
    public List<QAEventProcessed> searchByEventId(Context context, String eventId, Integer start, Integer size)
        throws SQLException;

    /**
     * Check if an event with the given checksum is already stored.
     * 
     * @param  context      the DSpace context
     * @param  checksum     the checksum to search for
     * @return              true if the given checksum is related to an already
     *                      stored event, false otherwise
     * @throws SQLException if an SQL error occurs
     */
    public boolean isEventStored(Context context, String checksum) throws SQLException;

    /**
     * Store an event related to the given checksum.
     *
     * @param  context  the DSpace context
     * @param  checksum the checksum of the event to be store
     * @param  eperson  the eperson who handle the event
     * @param  item     the item related to the event
     * @return          true if the creation is completed with success, false
     *                  otherwise
     */
    boolean storeEvent(Context context, String checksum, EPerson eperson, Item item);

}
