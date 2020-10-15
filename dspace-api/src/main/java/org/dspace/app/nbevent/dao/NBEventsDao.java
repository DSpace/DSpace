/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Repository;

public interface NBEventsDao {
		
	public List<NBEvent> searchByEventId(Context c, String eventId, Integer start, Integer size) throws SQLException;

	public boolean isEventStored(Context c, String checksum) throws SQLException;

	boolean storeEvent(Context c, String checksum, EPerson eperson, Item item);
	
}
