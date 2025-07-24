/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.LDNRouter;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.dao.LDNMessageDao;
import org.dspace.app.ldn.dao.NotifyServiceDao;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.model.NotifyRequestStatus;
import org.dspace.app.ldn.model.NotifyRequestStatusEnum;
import org.dspace.app.ldn.model.RequestStatus;
import org.dspace.app.ldn.model.Service;
import org.dspace.app.ldn.processor.LDNProcessor;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.ldn.utility.LDNUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableLDNNotification;
import org.dspace.event.Event;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation of {@link LDNMessageService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class LDNMessageServiceImpl implements LDNMessageService {

    @Autowired(required = true)
    private LDNMessageDao ldnMessageDao;
    @Autowired(required = true)
    private NotifyServiceDao notifyServiceDao;
    @Autowired(required = true)
    private ConfigurationService configurationService;
    @Autowired(required = true)
    private HandleService handleService;
    @Autowired(required = true)
    private ItemService itemService;
    @Autowired(required = true)
    private LDNRouter ldnRouter;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNMessageServiceImpl.class);
    private static final String LDN_ID_PREFIX = "urn:uuid:";

    protected LDNMessageServiceImpl() {

    }

    @Override
    public LDNMessageEntity find(Context context, String id) throws SQLException {

        if (id == null) {
            return null;
        }

        id = id.startsWith(LDN_ID_PREFIX) ? id : LDN_ID_PREFIX + id;
        return ldnMessageDao.findByID(context, LDNMessageEntity.class, id);
    }

    @Override
    public List<LDNMessageEntity> findAll(Context context) throws SQLException {
        return ldnMessageDao.findAll(context, LDNMessageEntity.class);
    }

    @Override
    public LDNMessageEntity create(Context context, String id) throws SQLException {
        LDNMessageEntity result = ldnMessageDao.findByID(context, LDNMessageEntity.class, id);
        if (result != null) {
            throw new SQLException("Duplicate LDN Message ID [" + id + "] detected. This message is rejected.");
        }
        return ldnMessageDao.create(context, new LDNMessageEntity(id));
    }

    @Override
    public LDNMessageEntity create(Context context, Notification notification, String sourceIp) throws SQLException {
        LDNMessageEntity ldnMessage = create(context, notification.getId());
        DSpaceObject obj = findDspaceObjectByUrl(context, notification.getObject().getId());
        if (obj == null) {
            if (isTargetCurrent(notification)) {
                // this means we're sending the notification
                obj = findDspaceObjectByUrl(context, notification.getObject().getAsObject());
                // use as:object for sender
            } else {
                // this means we're receiving the notification
                obj = findDspaceObjectByUrl(context, notification.getObject().getAsSubject());
                // use as:subject for receiver
            }
        }
        ldnMessage.setObject(obj);
        if (null != notification.getContext()) {
            ldnMessage.setContext(findDspaceObjectByUrl(context, notification.getContext().getId()));
        }
        ldnMessage.setOrigin(findNotifyService(context, notification.getOrigin()));
        ldnMessage.setInReplyTo(find(context, notification.getInReplyTo()));
        ObjectMapper mapper = new ObjectMapper();
        String message = null;
        try {
            message = mapper.writeValueAsString(notification);
            ldnMessage.setMessage(message);
        } catch (JsonProcessingException e) {
            log.error("Notification json can't be correctly processed " +
                "and stored inside the LDN Message Entity" + ldnMessage);
            log.error(e);
        }
        ldnMessage.setType(StringUtils.joinWith(",", notification.getType()));
        Set<String> notificationType = notification.getType();
        if (notificationType == null) {
            log.error("Notification has no notificationType attribute! " + notification);
            return null;
        }
        ArrayList<String> notificationTypeArrayList = new ArrayList<String>(notificationType);
        // sorting the list
        Collections.sort(notificationTypeArrayList);
        ldnMessage.setActivityStreamType(notificationTypeArrayList.get(0));
        if (notificationTypeArrayList.size() > 1) {
            ldnMessage.setCoarNotifyType(notificationTypeArrayList.get(1));
        } else {
            // The Notification's Type array does not include the CoarNotifyType information, e.g. ack notifications
            // Attempt to find it via the inReplyTo if present
            if (ldnMessage.getInReplyTo() != null) {
                ldnMessage.setCoarNotifyType(ldnMessage.getInReplyTo().getCoarNotifyType());
            }
        }
        ldnMessage.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED);
        ldnMessage.setSourceIp(sourceIp);
        if (ldnMessage.getOrigin() == null && !"Offer".equalsIgnoreCase(ldnMessage.getActivityStreamType())) {
            ldnMessage.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_UNTRUSTED);
        } else {

            boolean ipCheckRangeEnabled = configurationService.getBooleanProperty("ldn.ip-range.enabled", true);
            if (ipCheckRangeEnabled && !isValidIp(ldnMessage.getOrigin(), sourceIp)) {
                ldnMessage.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_UNTRUSTED_IP);
            }
        }
        ldnMessage.setQueueTimeout(Instant.now());

        update(context, ldnMessage);
        return ldnMessage;
    }

    @Override
    public boolean isValidIp(NotifyServiceEntity origin, String sourceIp) {

        String lowerIp = origin.getLowerIp();
        String upperIp = origin.getUpperIp();

        try {
            InetAddress ip = InetAddress.getByName(sourceIp);
            InetAddress lowerBoundAddress = InetAddress.getByName(lowerIp);
            InetAddress upperBoundAddress = InetAddress.getByName(upperIp);

            long ipLong = ipToLong(ip);
            long lowerBoundLong = ipToLong(lowerBoundAddress);
            long upperBoundLong = ipToLong(upperBoundAddress);

            return ipLong >= lowerBoundLong && ipLong <= upperBoundLong;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    @Override
    public void update(Context context, LDNMessageEntity ldnMessage) throws SQLException {
        // move the queue_status from UNTRUSTED to QUEUED if origin is a known NotifyService
        if (ldnMessage.getOrigin() != null &&
            LDNMessageEntity.QUEUE_STATUS_UNTRUSTED.compareTo(ldnMessage.getQueueStatus()) == 0) {
            ldnMessage.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED);
        }
        ldnMessageDao.save(context, ldnMessage);
        UUID notificationUUID = UUID.fromString(ldnMessage.getID().replace(LDN_ID_PREFIX, ""));
        ArrayList<String> identifiersList = new ArrayList<String>();
        identifiersList.add(ldnMessage.getID());
        context.addEvent(
            new Event(Event.MODIFY, Constants.LDN_MESSAGE,
                notificationUUID,
                IndexableLDNNotification.TYPE, identifiersList));
    }

    private DSpaceObject findDspaceObjectByUrl(Context context, String url) throws SQLException {
        String dspaceUrl = configurationService.getProperty("dspace.ui.url") + "/handle/";

        if (StringUtils.startsWith(url, dspaceUrl)) {
            return handleService.resolveToObject(context, url.substring(dspaceUrl.length()));
        }

        String handleResolver = configurationService.getProperty("handle.canonical.prefix", "https://hdl.handle.net/");
        if (StringUtils.startsWith(url, handleResolver)) {
            return handleService.resolveToObject(context, url.substring(handleResolver.length()));
        }

        dspaceUrl = configurationService.getProperty("dspace.ui.url") + "/items/";
        if (StringUtils.startsWith(url, dspaceUrl)) {
            return itemService.find(context, UUID.fromString(url.substring(dspaceUrl.length())));
        }

        return null;
    }

    public NotifyServiceEntity findNotifyService(Context context, Service service) throws SQLException {
        return notifyServiceDao.findByLdnUrl(context, service.getInbox());
    }

    @Override
    public List<LDNMessageEntity> findOldestMessagesToProcess(Context context) throws SQLException {
        List<LDNMessageEntity> result = null;
        int max_attempts = configurationService.getIntProperty("ldn.processor.max.attempts");
        result = ldnMessageDao.findOldestMessageToProcess(context, max_attempts);
        return result;
    }

    @Override
    public List<LDNMessageEntity> findMessagesToBeReprocessed(Context context) throws SQLException {
        List<LDNMessageEntity> result = null;
        result = ldnMessageDao.findMessagesToBeReprocessed(context);
        return result;
    }

    @Override
    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context) throws SQLException {
        List<LDNMessageEntity> result = null;
        int max_attempts = configurationService.getIntProperty("ldn.processor.max.attempts");
        result = ldnMessageDao.findProcessingTimedoutMessages(context, max_attempts);
        return result;
    }

    @Override
    public int extractAndProcessMessageFromQueue(Context context) throws SQLException {
        int count = 0;
        int timeoutInMinutes = configurationService.getIntProperty("ldn.processor.queue.msg.timeout", 60);

        List<LDNMessageEntity> messages = findOldestMessagesToProcess(context);
        messages.addAll(findMessagesToBeReprocessed(context));

        Optional<LDNMessageEntity> msgOpt = getSingleMessageEntity(messages);

        while (msgOpt.isPresent()) {
            LDNProcessor processor = null;
            LDNMessageEntity msg = msgOpt.get();
            processor = ldnRouter.route(msg);
            try {
                boolean isServiceDisabled = !isServiceEnabled(msg);
                if (processor == null || isServiceDisabled) {
                    log.warn("No processor found for LDN message " + msg);
                    Integer status = isServiceDisabled ? LDNMessageEntity.QUEUE_STATUS_UNTRUSTED
                        : LDNMessageEntity.QUEUE_STATUS_UNMAPPED_ACTION;
                    msg.setQueueStatus(status);
                    msg.setQueueAttempts(msg.getQueueAttempts() + 1);
                    update(context, msg);
                } else {
                    msg.setQueueLastStartTime(Instant.now());
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_PROCESSING);
                    msg.setQueueTimeout(Instant.now().plus(timeoutInMinutes, ChronoUnit.MINUTES));
                    update(context, msg);
                    ObjectMapper mapper = new ObjectMapper();
                    Notification notification = mapper.readValue(msg.getMessage(), Notification.class);
                    processor.process(context, notification);
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_PROCESSED);
                    count++;
                }
            } catch (JsonSyntaxException jse) {
                log.error("Unable to read JSON notification from LdnMessage " + msg, jse);
                msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
            } catch (Exception e) {
                log.error(e);
                msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
            } finally {
                msg.setQueueAttempts(msg.getQueueAttempts() + 1);
                update(context, msg);
            }

            messages = findOldestMessagesToProcess(context);
            messages.addAll(findMessagesToBeReprocessed(context));
            msgOpt = getSingleMessageEntity(messages);
        }
        return count;
    }

    private boolean isServiceEnabled(LDNMessageEntity msg) {
        String localInboxUrl = configurationService.getProperty("ldn.notify.inbox");
        if (msg.getTarget() == null || StringUtils.equals(msg.getTarget().getLdnUrl(), localInboxUrl)) {
            return msg.getOrigin().isEnabled();
        }
        return msg.getTarget().isEnabled();
    }

    @Override
    public int checkQueueMessageTimeout(Context context) throws SQLException {
        int count = 0;
        int maxAttempts = configurationService.getIntProperty("ldn.processor.max.attempts", 5);
        /*
         * put failed on processing messages with timed-out timeout and
         * attempts >= configured_max_attempts put queue on processing messages with
         * timed-out timeout and attempts < configured_max_attempts
         */
        Optional<LDNMessageEntity> msgOpt = getSingleMessageEntity(findProcessingTimedoutMessages(context));

        while (msgOpt.isPresent()) {
            LDNMessageEntity msg = msgOpt.get();
            try {
                if (msg.getQueueAttempts() >= maxAttempts) {
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
                } else {
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED);
                }
                update(context, msg);
                count++;
            } catch (SQLException e) {
                log.error("Can't update LDN message " + msg, e);
            }
            msgOpt = getSingleMessageEntity(findProcessingTimedoutMessages(context));
        }
        return count;
    }

    public Optional<LDNMessageEntity> getSingleMessageEntity(Collection<LDNMessageEntity> messages) {
        return messages.stream().findFirst();
    }

    @Override
    public NotifyRequestStatus findRequestsByItem(Context context, Item item) throws SQLException {
        NotifyRequestStatus result = new NotifyRequestStatus();
        result.setItemUuid(item.getID());
        List<LDNMessageEntity> msgs = ldnMessageDao.findAllMessagesByItem(
            context, item, "Offer");
        if (msgs != null && !msgs.isEmpty()) {
            for (LDNMessageEntity msg : msgs) {
                RequestStatus offer = new RequestStatus();
                NotifyServiceEntity nse = msg.getOrigin();
                if (nse == null) {
                    nse = msg.getTarget();
                }
                offer.setServiceName(nse == null ? "Unknown Service" : nse.getName());
                offer.setServiceUrl(nse == null ? "" : nse.getUrl());
                offer.setOfferType(LDNUtils.getNotifyType(msg.getCoarNotifyType()));
                List<LDNMessageEntity> acks = ldnMessageDao.findAllRelatedMessagesByItem(
                    context, msg, item, "Accept", "Reject", "TentativeReject", "TentativeAccept",
                        "Announce");
                if (acks == null || acks.isEmpty()) {
                    offer.setStatus(NotifyRequestStatusEnum.REQUESTED);
                } else if (acks.stream()
                        .filter(c -> (c.getActivityStreamType().equalsIgnoreCase("TentativeReject")))
                        .findAny().isPresent()) {
                    offer.setStatus(NotifyRequestStatusEnum.TENTATIVE_REJECT);
                } else if (acks.stream()
                        .filter(c -> (c.getActivityStreamType().equalsIgnoreCase("Reject")))
                        .findAny().isPresent()) {
                    offer.setStatus(NotifyRequestStatusEnum.REJECTED);
                } else if (acks.stream()
                    .filter(c -> (c.getActivityStreamType().equalsIgnoreCase("TentativeAccept") ||
                        c.getActivityStreamType().equalsIgnoreCase("Accept")))
                    .findAny().isPresent()) {
                    offer.setStatus(NotifyRequestStatusEnum.ACCEPTED);
                }
                if (acks.stream().filter(
                    c -> c.getActivityStreamType().equalsIgnoreCase("Announce"))
                    .findAny().isEmpty()) {
                    result.addRequestStatus(offer);
                }
            }
        }
        return result;
    }

    @Override
    public String findEndorsementOrReviewResubmissionIdByItem(Context context, Item item, NotifyServiceEntity service)
            throws SQLException {
        List<LDNMessageEntity> msgs = ldnMessageDao.findAllMessagesByItem(
                context, item, "TentativeReject");

        if (msgs != null && !msgs.isEmpty()) {
            for (LDNMessageEntity msg : msgs) {
                // Review and Endorsement are the only patterns supporting resubmissions at present
                if (msg.getCoarNotifyType().contains("EndorsementAction")
                        || msg.getCoarNotifyType().contains("ReviewAction")) {
                    // Only provide the resubmissionReplyTo UUID if the pattern supports resubmission
                    // Add an extra check to ensure that this is a resubmission: current notification service
                    // matches the service associated with a previous tentativeReject response. This is to avoid a
                    // case where a previous version of the item received a tentativeReject from one service
                    // and the author decides to submit the version to a different service, instead of a resubmission
                    if (msg.getOrigin() != null && msg.getOrigin().getID().equals(service.getID())) {
                        // Return the first ID found that will be used in the inReplyTo for a resubmission notification
                        return msg.getID();
                    }
                }
            }
        }
        return null;
    }

    public void delete(Context context, LDNMessageEntity ldnMessage) throws SQLException {
        ldnMessageDao.delete(context, ldnMessage);
    }

    @Override
    public boolean isTargetCurrent(Notification notification) {
        String localInboxUrl = configurationService.getProperty("ldn.notify.inbox");
        return StringUtils.equals(notification.getTarget().getInbox(), localInboxUrl);
    }

}
