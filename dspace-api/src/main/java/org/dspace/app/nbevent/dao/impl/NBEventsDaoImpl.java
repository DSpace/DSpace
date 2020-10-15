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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.dspace.app.nbevent.dao.NBEventsDao;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;


public class NBEventsDaoImpl extends AbstractHibernateDAO<NBEvent>
    implements NBEventsDao {

	@Override
	public boolean storeEvent(Context context, String checksum, EPerson eperson, Item item) {
		NBEvent nbEvent = new NBEvent();
		nbEvent.setEperson(eperson.getID().toString());
		nbEvent.setEventId(checksum);
		nbEvent.setItem(item.getID().toString());
		nbEvent.setEventTimestamp(new Date());
		try{
			save(context, nbEvent);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public boolean isEventStored(Context context, String checksum) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) " + "FROM " + NBEvent.class.getSimpleName()
            + " WHERE nbevent_id = :event_id ");
        query.setParameter("event_id", checksum);
		return count(query) != 0;
	}

	@Override
	public List<NBEvent> searchByEventId(Context context, String eventId, Integer start, Integer size) throws SQLException {
        Query query = createQuery(context, "SELECT * " + "FROM " + NBEvent.class.getSimpleName()
                + " WHERE nbevent_id = :event_id ");
        query.setFirstResult(start);
        query.setMaxResults(size);
        query.setParameter("event_id", eventId);
        return findMany(context, query);
	}

}
