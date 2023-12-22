/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation implementation for the {@link IndexableLDNNotification}
 *
 * @author Stefano Maffei at 4science.com
 */
public class LDNMessageEntityIndexFactoryImpl extends IndexFactoryImpl<IndexableLDNNotification, LDNMessageEntity> {

    @Autowired(required = true)
    private LDNMessageService ldnMessageService;
    @Autowired(required = true)
    private ItemService itemService;

    private static final String INCOMING = "Incoming";
    private static final String OUTGOING = "Outgoing";

    private static final Map<Integer, String> queueStatusMap;

    static {
        queueStatusMap = new HashMap<>();
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_QUEUED, "Queued");
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_PROCESSING, "Processing");
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_PROCESSED, "Processed");
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_FAILED, "Failure");
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_UNTRUSTED, "Untrusted");
        queueStatusMap.put(LDNMessageEntity.QUEUE_STATUS_UNMAPPED_ACTION, "Failure");
    }

    @Override
    public Iterator<IndexableLDNNotification> findAll(Context context) throws SQLException {
        final Iterator<LDNMessageEntity> ldnNotifications = ldnMessageService.findAll(context).iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return ldnNotifications.hasNext();
            }

            @Override
            public IndexableLDNNotification next() {
                return new IndexableLDNNotification(ldnNotifications.next());
            }
        };
    }

    @Override
    public String getType() {
        return IndexableLDNNotification.TYPE;
    }

    @Override
    public Optional<IndexableLDNNotification> findIndexableObject(Context context, String id) throws SQLException {
        final LDNMessageEntity ldnMessage = ldnMessageService.find(context, id);
        return ldnMessage == null ? Optional.empty() : Optional.of(new IndexableLDNNotification(ldnMessage));
    }

    @Override
    public boolean supports(Object object) {
        return object instanceof LDNMessageEntity;
    }

    @Override
    public List<IndexableLDNNotification> getIndexableObjects(Context context, LDNMessageEntity object)
        throws SQLException {
        return Arrays.asList(new IndexableLDNNotification(object));
    }

    @Override
    public SolrInputDocument buildDocument(Context context, IndexableLDNNotification indexableObject)
        throws SQLException, IOException {
        // Add the ID's, types and call the SolrServiceIndexPlugins
        final SolrInputDocument doc = super.buildDocument(context, indexableObject);
        final LDNMessageEntity ldnMessage = indexableObject.getIndexedObject();
        // add schema, element, qualifier and full fieldName
        doc.addField("notification_id", ldnMessage.getID());
        doc.addField("queue_status_i", ldnMessage.getQueueStatus());
        doc.addField("queue_status_s", queueStatusMap.get(ldnMessage.getQueueStatus()));
        doc.addField("notification_id", ldnMessage.getID());
        Item item = (Item) ldnMessage.getObject();
        if (item != null) {
            addFacetIndex(doc, "object", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
        }
        item = (Item) ldnMessage.getContext();
        if (item != null) {
            addFacetIndex(doc, "context", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
        }
        NotifyServiceEntity origin = ldnMessage.getOrigin();
        if (origin != null) {
            addFacetIndex(doc, "origin", String.valueOf(origin.getID()), getServiceNameForNotifyServ(origin));
        }
        NotifyServiceEntity target = ldnMessage.getOrigin();
        if (target != null) {
            addFacetIndex(doc, "target", String.valueOf(target.getID()), getServiceNameForNotifyServ(target));
        }
        if (ldnMessage.getInReplyTo() != null) {
            doc.addField("in_reply_to", ldnMessage.getInReplyTo().getID());
        }
        doc.addField("message", ldnMessage.getMessage());
        doc.addField("type", ldnMessage.getType());
        doc.addField("activity_stream_type", ldnMessage.getActivityStreamType());
        doc.addField("coar_notify_type", ldnMessage.getCoarNotifyType());
        doc.addField("queue_attempts", ldnMessage.getQueueAttempts());
        doc.addField("queue_last_start_time", ldnMessage.getQueueLastStartTime());
        doc.addField("queue_timeout", ldnMessage.getQueueTimeout());
        doc.addField("notification_type", getNotificationType(ldnMessage));

        return doc;
    }

    private String getNotificationType(LDNMessageEntity ldnMessage) {
        if (ldnMessage.getInReplyTo() != null || ldnMessage.getOrigin() != null) {
            return INCOMING;
        }
        return OUTGOING;
    }

    private String getServiceNameForNotifyServ(NotifyServiceEntity serviceEntity) {
        if (serviceEntity != null) {
            return serviceEntity.getName();
        }
        return "self";
    }

}
