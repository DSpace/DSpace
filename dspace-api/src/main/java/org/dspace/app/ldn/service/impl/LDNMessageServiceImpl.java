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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.time.DateUtils;
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
        ldnMessage.setObject(findDspaceObjectByUrl(context, notification.getObject().getId()));
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
        ldnMessage.setQueueTimeout(new Date());

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

        if (url.startsWith(dspaceUrl)) {
            return handleService.resolveToObject(context, url.substring(dspaceUrl.length()));
        }

        String handleResolver = configurationService.getProperty("handle.canonical.prefix", "https://hdl.handle.net/");
        if (url.startsWith(handleResolver)) {
            return handleService.resolveToObject(context, url.substring(handleResolver.length()));
        }

        dspaceUrl = configurationService.getProperty("dspace.ui.url") + "/items/";
        if (url.startsWith(dspaceUrl)) {
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
        int result = 0;
        int timeoutInMinutes = configurationService.getIntProperty("ldn.processor.queue.msg.timeout");
        if (timeoutInMinutes == 0) {
            timeoutInMinutes = 60;
        }
        List<LDNMessageEntity> msgs = new ArrayList<LDNMessageEntity>();
        try {
            msgs.addAll(findOldestMessagesToProcess(context));
            msgs.addAll(findMessagesToBeReprocessed(context));
            if (msgs != null && msgs.size() > 0) {
                LDNMessageEntity msg = null;
                LDNProcessor processor = null;
                for (int i = 0; processor == null && i < msgs.size() && msgs.get(i) != null; i++) {
                    processor = ldnRouter.route(msgs.get(i));
                    msg = msgs.get(i);
                    if (processor == null) {
                        log.info(
                            "No processor found for LDN message " + msgs.get(i));
                        msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_UNMAPPED_ACTION);
                        msg.setQueueAttempts(msg.getQueueAttempts() + 1);
                        update(context, msg);
                    }
                }
                if (processor != null) {
                    try {
                        msg.setQueueLastStartTime(new Date());
                        msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_PROCESSING);
                        msg.setQueueTimeout(DateUtils.addMinutes(new Date(), timeoutInMinutes));
                        update(context, msg);
                        ObjectMapper mapper = new ObjectMapper();
                        Notification notification = mapper.readValue(msg.getMessage(), Notification.class);
                        processor.process(context, notification);
                        msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_PROCESSED);
                        result = 1;
                    } catch (JsonSyntaxException jse) {
                        result = -1;
                        log.error("Unable to read JSON notification from LdnMessage " + msg, jse);
                        msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
                    } catch (Exception e) {
                        result = -1;
                        log.error(e);
                        msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
                    } finally {
                        msg.setQueueAttempts(msg.getQueueAttempts() + 1);
                        update(context, msg);
                    }
                } else {
                    log.info("Found x" + msgs.size() + " LDN messages but none processor found.");
                }
            }
        } catch (SQLException e) {
            result = -1;
            log.error(e);
        }
        return result;
    }

    @Override
    public int checkQueueMessageTimeout(Context context) {
        int result = 0;
        int timeoutInMinutes = configurationService.getIntProperty("ldn.processor.queue.msg.timeout");
        if (timeoutInMinutes == 0) {
            timeoutInMinutes = 60;
        }
        int maxAttempts = configurationService.getIntProperty("ldn.processor.max.attempts");
        if (maxAttempts == 0) {
            maxAttempts = 5;
        }
        log.debug("Using parameters: [timeoutInMinutes]=" + timeoutInMinutes + ",[maxAttempts]=" + maxAttempts);
        /*
         * put failed on processing messages with timed-out timeout and
         * attempts >= configured_max_attempts put queue on processing messages with
         * timed-out timeout and attempts < configured_max_attempts
         */
        List<LDNMessageEntity> msgsToCheck = null;
        try {
            msgsToCheck = findProcessingTimedoutMessages(context);
        } catch (SQLException e) {
            result = -1;
            log.error("An error occured on searching for timedout LDN messages!", e);
            return result;
        }
        if (msgsToCheck == null || msgsToCheck.isEmpty()) {
            return result;
        }
        for (int i = 0; i < msgsToCheck.size() && msgsToCheck.get(i) != null; i++) {
            LDNMessageEntity msg = msgsToCheck.get(i);
            try {
                if (msg.getQueueAttempts() >= maxAttempts) {
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_FAILED);
                } else {
                    msg.setQueueStatus(LDNMessageEntity.QUEUE_STATUS_QUEUED);
                }
                update(context, msg);
                result++;
            } catch (SQLException e) {
                log.error("Can't update LDN message " + msg);
                log.error(e);
            }
        }
        return result;
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
                offer.setServiceName(msg.getOrigin() == null ? "Unknown Service" : msg.getOrigin().getName());
                offer.setServiceUrl(msg.getOrigin() == null ? "" : msg.getOrigin().getUrl());
                offer.setOfferType(LDNUtils.getNotifyType(msg.getCoarNotifyType()));
                List<LDNMessageEntity> acks = ldnMessageDao.findAllRelatedMessagesByItem(
                    context, msg, item, "Accept", "TentativeReject", "TentativeAccept", "Announce");
                if (acks == null || acks.isEmpty()) {
                    offer.setStatus(NotifyRequestStatusEnum.REQUESTED);
                } else if (acks.stream()
                    .filter(c -> (c.getActivityStreamType().equalsIgnoreCase("TentativeAccept") ||
                        c.getActivityStreamType().equalsIgnoreCase("Accept")))
                    .findAny().isPresent()) {
                    offer.setStatus(NotifyRequestStatusEnum.ACCEPTED);
                } else if (acks.stream()
                    .filter(c -> c.getActivityStreamType().equalsIgnoreCase("TentativeReject"))
                    .findAny().isPresent()) {
                    offer.setStatus(NotifyRequestStatusEnum.REJECTED);
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

    public void delete(Context context, LDNMessageEntity ldnMessage) throws SQLException {
        ldnMessageDao.delete(context, ldnMessage);
    }

}
