/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.event.Event;

/**
 * Service interface for storing and retrieving DSpace Events in the audit system
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public interface AuditService {

    /**
     * Store a DSpace event in the audit core
     *
     * @param context the DSpace context
     * @param event the DSpace event to store
     * @throws SQLException if database error occurs
     */
    void store(Context context, Event event) throws SQLException;

    /**
     * Store an audit event
     *
     * @param audit the complete audit event to store, no details about the
     *              current user are extracted from the context
     */
    void store(AuditEvent audit);

    /**
     * Convert a DSpace Event into audit events. Please note that no user is
     * bound to an Event, if needed retrieve the current user from the context and
     * set it to the resulting Audit Event
     *
     * @param context the DSpace context
     * @param event the dspace event
     * @return a non-empty list of audit events wrapping the event without any user details
     */
    List<AuditEvent> getAuditEventsFromEvent(Context context, Event event);

    /**
     * Find all events with pagination support
     *
     * @param context DSpace context
     * @param limit the number of results to return
     * @param offset the offset for the pagination (0 based)
     * @param asc if true sort the result in ascending order (by timeStamp)
     * @return the list of audit event according to the pagination parameters
     */
    List<AuditEvent> findAllEvents(Context context, int limit, int offset, boolean asc);

    /**
     * Return the list of events in the specified time window for the requested object
     *
     * @param objectUuid can be null. If not null limit the audit events to the ones
     *                   where the subject or the object matches
     * @param from the start date (inclusive) can be null
     * @param to the end date (inclusive) can be null
     * @param limit the number of results to return
     * @param offset the offset for the pagination (0 based)
     * @param asc if true sort the result in ascending order (by timeStamp)
     * @return the list of events in the specified time window for the requested object
     */
    List<AuditEvent> findEvents(UUID objectUuid, Date from, Date to, int limit, int offset, boolean asc);

    /**
     * Find a specific audit event by its UUID
     *
     * @param context the DSpace context
     * @param id the UUID of the audit event
     * @return the audit event if found, null otherwise
     */
    AuditEvent findEvent(Context context, UUID id);

    /**
     * Delete events within the specified time range
     *
     * @param context the DSpace context
     * @param from the start date (inclusive)
     * @param to the end date (inclusive)
     */
    void deleteEvents(Context context, Date from, Date to);

    /**
     * Commit pending changes
     */
    void commit();

    /**
     * Count all audit events
     *
     * @param context the DSpace context
     * @return the total number of audit events
     */
    long countAllEvents(Context context);

    /**
     * Count events matching the specified criteria
     *
     * @param context the DSpace context
     * @param objectUuid can be null. If not null limit the count to events
     *                   where the subject or object matches
     * @param from the start date (inclusive) can be null
     * @param to the end date (inclusive) can be null
     * @return the number of events matching the criteria
     */
    long countEvents(Context context, UUID objectUuid, Date from, Date to);
}
