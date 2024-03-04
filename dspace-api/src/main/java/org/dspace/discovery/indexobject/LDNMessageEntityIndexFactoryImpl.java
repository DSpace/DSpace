/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import static org.apache.commons.lang3.time.DateFormatUtils.format;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
 * Factory implementation implementation for the
 * {@link IndexableLDNNotification}
 *
 * @author Stefano Maffei at 4science.com
 */
public class LDNMessageEntityIndexFactoryImpl extends IndexFactoryImpl<IndexableLDNNotification, LDNMessageEntity> {

    @Autowired(required = true)
    private LDNMessageService ldnMessageService;
    @Autowired(required = true)
    private ItemService itemService;

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
        doc.addField("queue_status_s", LDNMessageEntity.getQueueStatus(ldnMessage));
        addFacetIndex(doc, "queue_status", String.valueOf(ldnMessage.getQueueStatus()),
            LDNMessageEntity.getQueueStatus(ldnMessage));
        if (ldnMessage.getObject() != null && ldnMessage.getObject().getID() != null) {
            Item item = itemService.findByIdOrLegacyId(context, ldnMessage.getObject().getID().toString());
            if (item != null) {
                addFacetIndex(doc, "object", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
                addFacetIndex(doc, "relateditem", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
            }
        }
        if (ldnMessage.getContext() != null && ldnMessage.getContext().getID() != null) {
            Item item = itemService.findByIdOrLegacyId(context, ldnMessage.getContext().getID().toString());
            if (item != null) {
                addFacetIndex(doc, "context", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
                addFacetIndex(doc, "relateditem", item.getID().toString(), itemService.getMetadata(item, "dc.title"));
            }
        }
        NotifyServiceEntity origin = ldnMessage.getOrigin();
        if (origin != null) {
            addFacetIndex(doc, "origin", String.valueOf(origin.getID()),
                LDNMessageEntity.getServiceNameForNotifyServ(origin));
            addFacetIndex(doc, "ldn_service", String.valueOf(origin.getID()),
                LDNMessageEntity.getServiceNameForNotifyServ(origin));
        }
        NotifyServiceEntity target = ldnMessage.getTarget();
        if (target != null) {
            addFacetIndex(doc, "target", String.valueOf(target.getID()),
                LDNMessageEntity.getServiceNameForNotifyServ(target));
            addFacetIndex(doc, "ldn_service", String.valueOf(target.getID()),
                LDNMessageEntity.getServiceNameForNotifyServ(target));
        }
        if (ldnMessage.getInReplyTo() != null) {
            doc.addField("in_reply_to", ldnMessage.getInReplyTo().getID());
        }
        doc.addField("message", ldnMessage.getMessage());
        doc.addField("type", ldnMessage.getType());
        addFacetIndex(doc, "activity_stream_type", ldnMessage.getActivityStreamType(),
            ldnMessage.getActivityStreamType());
        addFacetIndex(doc, "coar_notify_type", ldnMessage.getCoarNotifyType(), ldnMessage.getCoarNotifyType());
        doc.addField("queue_attempts", ldnMessage.getQueueAttempts());
        doc.addField("queue_attempts_sort", ldnMessage.getQueueAttempts());

        indexDateFieldForFacet(doc, ldnMessage.getQueueLastStartTime());

        doc.addField("queue_timeout", ldnMessage.getQueueTimeout());
        String notificationType = LDNMessageEntity.getNotificationType(ldnMessage);
        addFacetIndex(doc, "notification_type", notificationType, notificationType);

        return doc;
    }

    private void indexDateFieldForFacet(SolrInputDocument doc, Date queueLastStartTime) {
        if (queueLastStartTime != null) {
            String value = format(queueLastStartTime, "yyyy-MM-dd");
            addFacetIndex(doc, "queue_last_start_time", value, value);
            doc.addField("queue_last_start_time", value);
            doc.addField("queue_last_start_time_dt", queueLastStartTime);
            doc.addField("queue_last_start_time_min", value);
            doc.addField("queue_last_start_time_min_sort", value);
            doc.addField("queue_last_start_time_max", value);
            doc.addField("queue_last_start_time_max_sort", value);
            doc.addField("queue_last_start_time.year",
                Integer.parseInt(format(queueLastStartTime, "yyyy")));
            doc.addField("queue_last_start_time.year_sort",
                Integer.parseInt(format(queueLastStartTime, "yyyy")));
        }
    }

}
