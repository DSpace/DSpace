/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

/**
 * Service to store and retrieve DSpace Events from the audit solr core
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.util.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to interact with the Solr audit core
 */
@Service
public class AuditService {
    private static final String UUID_FIELD = "uid";

    private static final String SUBJECT_UUID_FIELD = "subject_uuid";

    private static final String SUBJECT_TYPE_FIELD = "subject_type";

    private static final String OBJECT_UUID_FIELD = "object_uuid";

    private static final String OBJECT_TYPE_FIELD = "object_type";

    private static final String EVENT_TYPE_FIELD = "event_type";

    private static final String EPERSON_UUID_FIELD = "eperson_uuid";

    private static final String DATETIME_FIELD = "timeStamp";

    private static final String DETAIL_FIELD = "detail";

    @Autowired
    private ConfigurationService configurationService;

    private Logger log = LogManager.getLogger(AuditService.class);

    protected SolrClient solr = null;

    protected SolrClient getSolr() throws MalformedURLException, SolrServerException, IOException {
        if (solr == null) {
            String solrService = configurationService.getProperty("solr.audit.server");
            log.debug("Solr audit URL: " + solrService);
            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();
            solrServer.setBaseURL(solrService);
            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
            solrServer.query(solrQuery);
            solr = solrServer;
        }
        return solr;
    }

    public void store(Context context, Event event) {
        AuditEvent audit = getAuditEventFromEvent(event);
        EPerson eperson = context.getCurrentUser();
        if (eperson != null) {
            audit.setEpersonUUID(eperson.getID());
        }
        store(context, audit);
    }

    /**
     * Store an audit event as is in the Solr audit core
     * 
     * @param context DSpace Context
     * @param audit   the complete audit event to store, no details about the
     *                current user are extracted from the context
     */
    public void store(Context context, AuditEvent audit) {
        SolrInputDocument solrInDoc = new SolrInputDocument();
        // this is usually NOT the case, as the audit event get a random uuid by solr
        // but it is convenient for testing purpose
        if (audit.getUuid() != null) {
            solrInDoc.addField(UUID_FIELD, audit.getUuid().toString());
        }
        solrInDoc.addField(SUBJECT_UUID_FIELD, audit.getSubjectUUID().toString());
        solrInDoc.addField(SUBJECT_TYPE_FIELD, audit.getSubjectType());
        if (audit.getObjectUUID() != null) {
            solrInDoc.addField(OBJECT_UUID_FIELD, audit.getObjectUUID().toString());
            solrInDoc.addField(OBJECT_TYPE_FIELD, audit.getObjectType());
        }
        solrInDoc.addField(EVENT_TYPE_FIELD, audit.getEventType());
        if (audit.getEpersonUUID() != null) {
            solrInDoc.addField(EPERSON_UUID_FIELD, audit.getEpersonUUID().toString());
        }
        solrInDoc.addField(DATETIME_FIELD, audit.getDatetime());
        if (audit.getDetail() != null) {
            solrInDoc.addField(DETAIL_FIELD, audit.getDetail());
        }
        try {
            getSolr().add(solrInDoc);
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * This method convert an Event in an audit event. Please note that no user is
     * bound to an Event, if needed retrieve the current user from the context and
     * set it to the resulting Audit Event
     * 
     * @param event the dspace event
     * @return an audit event wrapping the event without any user details
     */
    public AuditEvent getAuditEventFromEvent(Event event) {
        AuditEvent audit = new AuditEvent();
        audit.setDatetime(new Date(event.getTimeStamp()));
        audit.setEventType(event.getEventTypeAsString());
        if (event.getObjectID() != null) {
            audit.setObjectType(event.getObjectTypeAsString());
            audit.setObjectUUID(event.getObjectID());
        }
        audit.setSubjectType(event.getSubjectTypeAsString());
        audit.setSubjectUUID(event.getSubjectID());
        return audit;
    }
    /**
     * Shortcut for
     * {@link #findEvents(Context, UUID, Date, Date, int, int, boolean)} with
     * objectUuid, from and to null
     * 
     * @param context DSpace context
     * @param limit   the number of results to return
     * @param offset  the offset for the pagination (0 based)
     * @param asc     if true sort the result in ascending order (by timeStamp)
     * @return the list of audit event according to the pagination parameters
     */
    public List<AuditEvent> findAllEvents(Context context, int limit, int offset,
            boolean asc) {
        return findEvents(context, null, null, null, limit, offset, asc);
    }

    /**
     * Return the list of events in the specified time window for the requested
     * object
     * 
     * @param context    DSpace context
     * @param objectUuid can be null. If not null limit the audit events to the ones
     *                   where the subject or the object
     * @param from       the start date (inclusive) can be null
     * @param to         the end date (inclusive) can be null
     * @param limit      the number of results to return
     * @param offset     the offset for the pagination (0 based)
     * @param asc        if true sort the result in ascending order (by timeStamp)
     * @return the list of events in the specified time window for the requested
     *         object
     */
    public List<AuditEvent> findEvents(Context context, UUID objectUuid, Date from, Date to, int limit, int offset,
            boolean asc) {
        String q = "*";
        if (objectUuid != null) {
            q = objectUuid.toString();
        }
        SolrQuery solrQuery = new SolrQuery("(" + SUBJECT_UUID_FIELD + ":" + q + " OR "
                + OBJECT_UUID_FIELD + ":" + q + ")  AND " + buildTimeQuery(from, to));
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.addSort(new SortClause(DATETIME_FIELD, asc ? ORDER.asc : ORDER.desc));
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);
        QueryResponse queryResponse;
        try {
            queryResponse = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        List<AuditEvent> listResourceSyncEvent = new ArrayList<AuditEvent>();
        for (SolrDocument sd : queryResponse.getResults()) {
            AuditEvent rse = getAuditEventFromSolrDoc(sd);
            listResourceSyncEvent.add(rse);
        }
        return listResourceSyncEvent;
    }

    public AuditEvent findEvent(Context context, UUID id) {
        SolrQuery solrQuery = new SolrQuery(UUID_FIELD + ":" + id.toString());
        QueryResponse queryResponse;
        try {
            queryResponse = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        for (SolrDocument sd : queryResponse.getResults()) {
            AuditEvent rse = getAuditEventFromSolrDoc(sd);
            return rse;
        }
        return null;
    }

    private AuditEvent getAuditEventFromSolrDoc(SolrDocument sd) {
        AuditEvent rse = new AuditEvent();
        rse.setUuid(UUID.fromString((String) sd.getFieldValue(UUID_FIELD)));
        if (sd.getFieldValue(OBJECT_UUID_FIELD) != null) {
            rse.setObjectUUID(UUID.fromString((String) sd.getFieldValue(OBJECT_UUID_FIELD)));
            rse.setObjectType((String) sd.getFieldValue(OBJECT_TYPE_FIELD));
        }
        if (sd.getFieldValue(SUBJECT_UUID_FIELD) != null) {
            rse.setSubjectUUID(UUID.fromString((String) sd.getFieldValue(SUBJECT_UUID_FIELD)));
            rse.setSubjectType((String) sd.getFieldValue(SUBJECT_TYPE_FIELD));
        }
        if (sd.getFieldValue(EPERSON_UUID_FIELD) != null) {
            rse.setEpersonUUID(UUID.fromString((String) sd.getFieldValue(EPERSON_UUID_FIELD)));
        }
        rse.setEventType((String) sd.getFieldValue(EVENT_TYPE_FIELD));
        rse.setDatetime((Date) sd.getFieldValue(DATETIME_FIELD));
        rse.setDetail((String) sd.getFieldValue(DETAIL_FIELD));
        return rse;
    }

    /**
     * Return the audit event for the specified uuid if any
     * 
     * @param context the DSpace Context
     * @param uuid    the uuid of the Audit Event
     * @return the audit event for the specified uuid if any
     */
    public AuditEvent getAuditEvent(Context context, UUID uuid) {
        SolrQuery solrQuery = new SolrQuery(UUID_FIELD + ":" + uuid.toString());
        QueryResponse queryResponse;
        try {
            queryResponse = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        for (SolrDocument sd : queryResponse.getResults()) {
            AuditEvent rse = getAuditEventFromSolrDoc(sd);
            return rse;
        }
        return null;

    }

    public void deleteEvents(Context context, Date from, Date to) {
        try {
            getSolr().deleteByQuery(buildTimeQuery(from, to));
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void commit() {
        try {
            getSolr().commit();
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String buildTimeQuery(Date from, Date to) {
        String fromDate;
        if (from == null) {
            fromDate = "*";
        } else {
            fromDate = SolrUtils.getDateFormatter().format(from);
        }
        String toDate;
        if (to == null) {
            toDate = "*";
        } else {
            toDate = SolrUtils.getDateFormatter().format(to);
        }
        return DATETIME_FIELD + ":[" + fromDate + " TO " + toDate + "]";
    }

    public long countAllEvents(Context context) {
        return countEvents(context, null, null, null);
    }

    public long countEvents(Context context, UUID objectUuid, Date from, Date to) {
        String q = "*";
        if (objectUuid != null) {
            q = objectUuid.toString();
        }
        SolrQuery solrQuery = new SolrQuery("(" + SUBJECT_UUID_FIELD + ":" + q + " OR "
                + OBJECT_UUID_FIELD + ":" + q + ")  AND " + buildTimeQuery(from, to));
        solrQuery.setRows(Integer.MAX_VALUE);
        solrQuery.setRows(0);
        QueryResponse queryResponse;
        try {
            queryResponse = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return queryResponse.getResults().getNumFound();
    }

}
