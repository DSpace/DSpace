/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Query;
import org.dspace.content.Item;
import org.dspace.content.QAEventProcessed;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.qaevent.dao.QAEventsDAO;

/**
 * Implementation of {@link QAEventsDAO} that store processed events using an
 * SQL DBMS.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventsDAOImpl extends AbstractHibernateDAO<QAEventProcessed> implements QAEventsDAO {

    @Override
    public List<QAEventProcessed> findAll(Context context) throws SQLException {
        return findAll(context, QAEventProcessed.class);
    }

    @Override
    public boolean storeEvent(Context context, String checksum, EPerson eperson, Item item) {
        QAEventProcessed qaEvent = new QAEventProcessed();
        qaEvent.setEperson(eperson);
        qaEvent.setEventId(checksum);
        qaEvent.setItem(item);
        qaEvent.setEventTimestamp(new Date());
        try {
            create(context, qaEvent);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean isEventStored(Context context, String checksum) throws SQLException {
        Query query = createQuery(context,
                "SELECT count(eventId) FROM QAEventProcessed qaevent WHERE qaevent.eventId = :event_id ");
        query.setParameter("event_id", checksum);
        return count(query) != 0;
    }

    @Override
    public List<QAEventProcessed> searchByEventId(Context context, String eventId, Integer start, Integer size)
            throws SQLException {
        Query query = createQuery(context,
                "SELECT * FROM QAEventProcessed qaevent WHERE qaevent.qaevent_id = :event_id ");
        query.setFirstResult(start);
        query.setMaxResults(size);
        query.setParameter("event_id", eventId);
        return findMany(context, query);
    }

    @Override
    public List<QAEventProcessed> findByItem(Context context, Item item) throws SQLException {
        Query query = createQuery(context, ""
            + " SELECT qaevent "
            + " FROM QAEventProcessed qaevent "
            + " WHERE qaevent.item = :item ");
        query.setParameter("item", item);
        return findMany(context, query);
    }

    @Override
    public List<QAEventProcessed> findByEPerson(Context context, EPerson ePerson) throws SQLException {
        Query query = createQuery(context, ""
            + " SELECT qaevent "
            + " FROM QAEventProcessed qaevent "
            + " WHERE qaevent.eperson = :eperson ");
        query.setParameter("eperson", ePerson);
        return findMany(context, query);
    }

}
