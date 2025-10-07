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

import static org.dspace.app.audit.MetadataEvent.INITIAL_ADD;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.DetailType;
import org.dspace.event.Event;
import org.dspace.event.EventDetail;
import org.dspace.services.ConfigurationService;
import org.dspace.util.SolrUtils;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to interact with the Solr audit core
 */
@Service
public class AuditService {
    // field names in the solr core
    // uid is not a typo it is the field name in the solr schema
    private static final String UUID_FIELD = "uid";
    private static final String SUBJECT_UUID_FIELD = "subject_uuid";
    private static final String SUBJECT_TYPE_FIELD = "subject_type";
    private static final String OBJECT_UUID_FIELD = "object_uuid";
    private static final String OBJECT_TYPE_FIELD = "object_type";
    private static final String EVENT_TYPE_FIELD = "event_type";
    private static final String EPERSON_UUID_FIELD = "eperson_uuid";
    private static final String DATETIME_FIELD = "timeStamp";
    private static final String DETAIL_FIELD = "detail";
    private static final String METADATA_FIELD = "metadata_field";
    private static final String VALUE_FIELD = "value";
    private static final String AUTHORITY_FIELD = "authority";
    private static final String CONFIDENCE_FIELD = "confidence";
    private static final String PLACE_FIELD = "place";
    private static final String ACTION_FIELD = "action";
    private static final String CHECKSUM = "checksum";

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PoolTaskService poolTaskService;
    @Autowired
    private WorkspaceItemService workspaceItemService;

    /**
     * Dedicated logger used only for emitting raw audit events with metadata & checksum details.
     * Enable/disable independently via loglevel.audit.events. Falls back gracefully if disabled.
     */
    private static final Logger AUDIT_EVENT_LOGGER = LogManager.getLogger("org.dspace.app.audit.event");

    private static Logger log = LogManager.getLogger(AuditService.class);

    protected SolrClient solr = null;

    protected SolrClient getSolr() throws MalformedURLException, SolrServerException, IOException {
        if (solr == null) {
            String solrService = configurationService.getProperty("solr.audit.server");
            log.debug("Solr audit URL: " + solrService);
            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();
            solrServer.setBaseURL(solrService);
            SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
            // checking SOLR is alive
            solrServer.query(solrQuery);
            solr = solrServer;
        }
        return solr;
    }

    public void store(Context context, Event event) throws SQLException {
        if (!isProcessableEvent(event)) {
            return;
        }

        if (Event.DELETE != event.getEventType() && !isAuditableItem(context, event)) {
            return;
        }
        List<AuditEvent> audits = getAuditEventsFromEvent(context, event);
        for (AuditEvent audit: audits) {
            if ("MODIFY_METADATA".equals(event.getEventTypeAsString()) &&
                StringUtils.isEmpty(audit.getMetadataField())) {
                continue;
            }

            store(context, audit);
        }
    }

    private boolean isProcessableEvent(Event event) {
        return event.getDetail() != null && (DetailType.BITSTREAM_CHECKSUM.equals(event.getDetail().getDetailKey())
            || DetailType.DSO_SUMMARY.equals(event.getDetail().getDetailKey())
            || Event.DELETE == event.getEventType());
    }

    private boolean isAuditableItem(Context context, Event event) throws SQLException {
        if (event.getSubjectType() != Constants.ITEM && event.getSubjectType() != Constants.BITSTREAM
            && event.getSubjectType() != Constants.BUNDLE) {
            return true;
        }

        Item item = retrieveItem(context, event);
        if (item == null) {
            return true;
        }

        if (item.isArchived()) {
            return true;
        }

        boolean result = false;
        if (configurationService.getBooleanProperty("audit.item.in-workflow")) {
            result = poolTaskService.findAll(context).stream()
                .anyMatch(pt -> StringUtils.equalsIgnoreCase(pt.getWorkflowItem().getItem().getID().toString(),
                    item.getID().toString()));
        }

        if (!result && configurationService.getBooleanProperty("audit.item.in-workspace")) {
            result = workspaceItemService.findAll(context).stream()
                .anyMatch(wi -> StringUtils.equalsIgnoreCase(wi.getItem().getID().toString(),
                    item.getID().toString()));
        }

        return result;
    }

    private Item retrieveItem(Context context, Event event) throws SQLException {
        int subjectType = event.getSubjectType();
        Object subject = event.getSubject(context);
        if (subject == null) {
            log.debug("Subject is null for event with subjectType {}", subjectType);
            return null;
        }
        try {
            switch (subjectType) {
                case Constants.BITSTREAM: {
                    Bitstream bitstream = (Bitstream) subject;
                    if (CollectionUtils.isEmpty(bitstream.getBundles())) {
                        log.debug("Bitstream has no bundles, cannot resolve parent Item");
                        return null;
                    }
                    Bundle firstBundle = bitstream.getBundles().get(0);
                    if (CollectionUtils.isEmpty(firstBundle.getItems())) {
                        log.debug("First bundle has no items, cannot resolve Item");
                        return null;
                    }
                    return firstBundle.getItems().get(0);
                }
                case Constants.BUNDLE: {
                    Bundle bundle = (Bundle) subject;
                    if (CollectionUtils.isEmpty(bundle.getItems())) {
                        log.debug("Bundle has no items, cannot resolve Item");
                        return null;
                    }
                    return bundle.getItems().get(0);
                }
                case Constants.ITEM: {
                    return (Item) subject;
                }
                default: {
                    log.debug("retrieveItem called with unsupported subjectType: {}", subjectType);
                    return null;
                }
            }
        } catch (ClassCastException cce) {
            log.warn("Subject cannot be cast to expected type for subjectType {}: {}",
                    subjectType, subject.getClass(), cce);
            return null;
        }
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

        if (audit.getMetadataField() != null) {
            solrInDoc.addField(METADATA_FIELD, audit.getMetadataField());
            solrInDoc.addField(VALUE_FIELD, audit.getValue());
            solrInDoc.addField(AUTHORITY_FIELD, audit.getAuthority() == null ? "" : audit.getAuthority());
            solrInDoc.addField(CONFIDENCE_FIELD, audit.getConfidence());
            solrInDoc.addField(PLACE_FIELD, audit.getPlace());
            solrInDoc.addField(ACTION_FIELD, audit.getAction());
        }

        if (StringUtils.isNotEmpty(audit.getChecksum())) {
            solrInDoc.addField(CHECKSUM, audit.getChecksum());
        }

        try {
            getSolr().add(solrInDoc);
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
        // Emit dedicated audit event log line if enabled
        if (AUDIT_EVENT_LOGGER.isEnabled(Level.ALL)) {
            StringBuilder sb = new StringBuilder();
            sb.append("AUDIT_EVENT")
              .append(' ').append("eventType=").append(audit.getEventType())
              .append(' ').append("subjectUUID=").append(audit.getSubjectUUID())
              .append(' ').append("subjectType=").append(audit.getSubjectType())
              .append(' ').append("objectUUID=").append(audit.getObjectUUID())
              .append(' ').append("objectType=").append(audit.getObjectType())
              .append(' ').append("metadataField=").append(nullSafe(audit.getMetadataField()))
              .append(' ').append("value=").append(nullSafe(audit.getValue()))
              .append(' ').append("authority=").append(nullSafe(audit.getAuthority()))
              .append(' ').append("confidence=").append(audit.getConfidence())
              .append(' ').append("place=").append(audit.getPlace())
              .append(' ').append("action=").append(nullSafe(audit.getAction()))
              .append(' ').append("checksum=").append(nullSafe(audit.getChecksum()))
              .append(' ').append("datetime=").append(audit.getDatetime() == null ?
                            "null" : audit.getDatetime().getTime())
              .append(' ').append("epersonUUID=").append(audit.getEpersonUUID());
            AUDIT_EVENT_LOGGER.info(sb.toString());
        }
    }

    /**
     * Utility to avoid NPEs in log lines.
     */
    private String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }


    /**
     * This method convert an Event in an audit event. Please note that no user is
     * bound to an Event, if needed retrieve the current user from the context and
     * set it to the resulting Audit Event
     * 
     * @param event the dspace event
     * @return a non-empty list of audit events wrapping the event without any user details
     */
    public List<AuditEvent> getAuditEventsFromEvent(Context context, Event event) {
        List<AuditEvent> audits = new ArrayList<>();
        List<MetadataEvent> metadataEvents = extractMetadataDetail(event.getDetail());

        for (MetadataEvent metadataEvent : metadataEvents) {
            audits.add(getAuditEvent(context, event, metadataEvent));
        }

        if (CollectionUtils.isEmpty(audits)) {
            audits.add(getAuditEvent(context, event, null));
        }

        return audits;
    }

    private List<MetadataEvent> extractMetadataDetail(EventDetail detail) {
        try {
            if (detail == null || detail.getDetailObject() == null ||
                !detail.getDetailKey().equals(DetailType.DSO_SUMMARY)) {
                return List.of();
            }

            List<Object> details = (List<Object>)detail.getDetailObject();
            List<MetadataEvent> metadataEvents = details.stream().filter(obj -> obj instanceof MetadataEvent)
                                          .map(obj -> (MetadataEvent) obj)
                                          .toList();

            return metadataEvents.stream()
                                 .filter(metadataEvent ->
                                     !metadataEvent.getAction().equals(INITIAL_ADD))
                                 .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractChecksumDetail(EventDetail detail) {
        if (detail == null || detail.getDetailObject() == null ||
            !detail.getDetailKey().equals(DetailType.BITSTREAM_CHECKSUM)) {
            return "";
        }
        return (String)detail.getDetailObject();
    }

    private AuditEvent getAuditEvent(Context context, Event event, MetadataEvent metadataEvent) {
        AuditEvent audit = new AuditEvent();
        audit.setDatetime(new Date(event.getTimeStamp()));
        audit.setEventType(event.getEventTypeAsString());

        EPerson eperson = context.getCurrentUser();
        if (eperson != null) {
            audit.setEpersonUUID(eperson.getID());
        }

        if (event.getObjectID() != null) {
            audit.setObjectType(event.getObjectTypeAsString());
            audit.setObjectUUID(event.getObjectID());
        }

        audit.setSubjectType(event.getSubjectTypeAsString());
        audit.setSubjectUUID(event.getSubjectID());

        if (metadataEvent != null) {
            audit.setMetadataField(metadataEvent.getMetadataField());
            audit.setValue(metadataEvent.getValue());
            audit.setAuthority(metadataEvent.getAuthority());
            audit.setConfidence(metadataEvent.getConfidence());
            audit.setPlace(metadataEvent.getPlace());
            audit.setAction(metadataEvent.getAction());
        }

        String checksum = extractChecksumDetail(event.getDetail());
        if (!checksum.isEmpty()) {
            audit.setChecksum(checksum);
        }

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
        rse.setMetadataField((String) sd.getFieldValue(METADATA_FIELD));
        rse.setValue((String) sd.getFieldValue(VALUE_FIELD));
        rse.setAuthority((String) sd.getFieldValue(AUTHORITY_FIELD));
        rse.setConfidence((Integer) sd.getFieldValue(CONFIDENCE_FIELD));
        rse.setPlace((Integer) sd.getFieldValue(PLACE_FIELD));
        rse.setAction((String) sd.getFieldValue(ACTION_FIELD));
        rse.setChecksum((String) sd.getFieldValue(CHECKSUM));
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
            fromDate = SolrUtils.getDateFormatter().format(
                from.toInstant().atZone(java.time.ZoneId.systemDefault())
            );
        }
        String toDate;
        if (to == null) {
            toDate = "*";
        } else {
            toDate = SolrUtils.getDateFormatter().format(
                to.toInstant().atZone(java.time.ZoneId.systemDefault())
            );
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
        QueryResponse queryResponse;
        try {
            queryResponse = getSolr().query(solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        return queryResponse.getResults().getNumFound();
    }

}
