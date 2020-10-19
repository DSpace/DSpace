/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.persistence.Query;

import org.dspace.app.nbevent.dao.NBEventsDao;
import org.dspace.content.Item;
import org.dspace.content.NBEventProcessed;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class NBEventsDaoImpl extends AbstractHibernateDAO<NBEventProcessed> implements NBEventsDao {

    @Override
    public boolean storeEvent(Context context, String checksum, EPerson eperson, Item item) {
        NBEventProcessed nbEvent = new NBEventProcessed();
        nbEvent.setEperson(eperson);
        nbEvent.setEventId(checksum);
        nbEvent.setItem(item);
        nbEvent.setEventTimestamp(new Date());
        try {
            save(context, nbEvent);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean isEventStored(Context context, String checksum) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(eventId) FROM NBEventProcessed nbevent WHERE nbevent.eventId = :event_id ");
        query.setParameter("event_id", checksum);
        return count(query) != 0;
    }

    @Override
    public List<NBEventProcessed> searchByEventId(Context context, String eventId, Integer start, Integer size)
            throws SQLException {
        Query query = createQuery(context,
                "SELECT * " + "FROM NBEventProcessed nbevent WHERE nbevent.nbevent_id = :event_id ");
        query.setFirstResult(start);
        query.setMaxResults(size);
        query.setParameter("event_id", eventId);
        return findMany(context, query);
    }

}
