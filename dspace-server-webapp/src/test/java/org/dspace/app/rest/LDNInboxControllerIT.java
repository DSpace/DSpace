/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.content.QAEvent.COAR_NOTIFY_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.matcher.QASourceMatcher;
import org.dspace.matcher.QATopicMatcher;
import org.dspace.qaevent.QANotifyPatterns;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.RequestPostProcessor;


/**
 * LDN Controller test class. Simulate receiving external LDN messages
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */

public class LDNInboxControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LDNMessageService ldnMessageService;

    private QAEventService qaEventService = new DSpace().getSingletonService(QAEventService.class);

    @Test
    public void ldnInboxAnnounceEndorsementTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.3")
                                .build();
        context.restoreAuthSystemState();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        String message = announceEndorsement.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());

        LDNMessageEntity ldnMessage = ldnMessageService.find(context, notification.getId());
        checkStoredLDNMessage(notification, ldnMessage, object);
    }

    @Test
    public void ldnInboxAnnounceReviewTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        InputStream announceReviewStream = getClass().getResourceAsStream("ldn_announce_review.json");
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://review-service.com/inbox/about/")
                                .withLdnUrl("https://review-service.com/inbox/")
                                .withScore(BigDecimal.valueOf(0.6d))
                                .withStatus(true)
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.3")
                                .build();
        String announceReview = IOUtils.toString(announceReviewStream, Charset.defaultCharset());
        announceReviewStream.close();
        String message = announceReview.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());

        ldnMessageService.extractAndProcessMessageFromQueue(context);

        assertThat(qaEventService.findAllSources(context, 0, 20),
            hasItem(QASourceMatcher.with(COAR_NOTIFY_SOURCE, 1L)));

        assertThat(qaEventService.findAllTopicsBySource(context, COAR_NOTIFY_SOURCE, 0, 20, "topic", true), hasItem(
            QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW, 1L)));

    }

    @Test
    public void ldnInboxEndorsementActionBadRequestTest() throws Exception {
        // id is not an uri
        InputStream offerEndorsementStream = getClass().getResourceAsStream("ldn_offer_endorsement_badrequest.json");
        String message = IOUtils.toString(offerEndorsementStream, Charset.defaultCharset());
        offerEndorsementStream.close();
        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void ldnInboxOfferReviewAndACKTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://review-service.com/inbox/about/")
                                .withLdnUrl("https://review-service.com/inbox/")
                                .withScore(BigDecimal.valueOf(0.6d))
                                .withStatus(true)
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.3")
                                .build();
        InputStream offerReviewStream = getClass().getResourceAsStream("ldn_offer_review.json");
        String announceReview = IOUtils.toString(offerReviewStream, Charset.defaultCharset());
        offerReviewStream.close();
        String message = announceReview.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());

        int processed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(processed, 1);
        processed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(processed, 0);

        InputStream ackReviewStream = getClass().getResourceAsStream("ldn_ack_review_reject.json");
        String ackReview = IOUtils.toString(ackReviewStream, Charset.defaultCharset());
        offerReviewStream.close();
        String ackMessage = ackReview.replaceAll("<<object_handle>>", object);
        ackMessage = ackMessage.replaceAll("<<ldn_offer_review_uuid>>",
            "urn:uuid:0370c0fb-bb78-4a9b-87f5-bed307a509de");
        ObjectMapper ackMapper = new ObjectMapper();
        Notification ackNotification = mapper.readValue(ackMessage, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(ackMessage))
            .andExpect(status().isAccepted());

        int ackProcessed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(ackProcessed, 1);
        ackProcessed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(ackProcessed, 0);


    }

    @Test
    public void ldnInboxAnnounceReleaseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        InputStream announceRelationshipStream = getClass().getResourceAsStream("ldn_announce_release.json");
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://review-service.com/inbox/about/")
                                .withLdnUrl("https://review-service.com/inbox/")
                                .withScore(BigDecimal.valueOf(0.6d))
                                .withStatus(true)
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.3")
                                .build();
        String announceRelationship = IOUtils.toString(announceRelationshipStream, Charset.defaultCharset());
        announceRelationshipStream.close();
        String message = announceRelationship.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());

        ldnMessageService.extractAndProcessMessageFromQueue(context);

        assertThat(qaEventService.findAllSources(context, 0, 20),
            hasItem(QASourceMatcher.with(COAR_NOTIFY_SOURCE, 1L)));

        assertThat(qaEventService.findAllTopicsBySource(context, COAR_NOTIFY_SOURCE, 0, 20, "topic", true), hasItem(
            QATopicMatcher.with(QANotifyPatterns.TOPIC_ENRICH_MORE_LINK, 1L)));

    }

    private void checkStoredLDNMessage(Notification notification, LDNMessageEntity ldnMessage, String object)
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Notification storedMessage = mapper.readValue(ldnMessage.getMessage(), Notification.class);

        assertNotNull(ldnMessage);
        assertNotNull(ldnMessage.getObject());
        assertEquals(ldnMessage.getObject()
                               .getMetadata()
                               .stream()
                               .filter(metadataValue ->
                                   metadataValue.getMetadataField().toString('.').equals("dc.identifier.uri"))
                               .map(metadataValue -> metadataValue.getValue())
                               .findFirst().get(), object);

        assertEquals(notification.getId(), storedMessage.getId());
        assertEquals(notification.getOrigin().getInbox(), storedMessage.getOrigin().getInbox());
        assertEquals(notification.getTarget().getInbox(), storedMessage.getTarget().getInbox());
        assertEquals(notification.getObject().getId(), storedMessage.getObject().getId());
        assertEquals(notification.getType(), storedMessage.getType());
    }

    @Test
    public void ldnInboxAnnounceEndorsementInvalidIpTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.1")
                                .build();
        context.restoreAuthSystemState();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        String message = announceEndorsement.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox").with(remoteHost("mydocker.url", "172.23.0.1"))
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isBadRequest());

        int processed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(processed, 0);
        LDNMessageEntity ldnMessage = ldnMessageService.find(context, notification.getId());
        assertNull(ldnMessage);
    }

    @Test
    public void ldnInboxAnnounceEndorsementInvalidInboxTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
                                .withLowerIp("127.0.0.2")
                                .withUpperIp("127.0.0.5")
                                .build();
        context.restoreAuthSystemState();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_origin_inbox_unregistered.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        String message = announceEndorsement.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
            .contentType("application/ld+json")
            .content(message))
            .andExpect(status().isBadRequest());

        int processed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(processed, 0);

    }

    @Test
    public void ldnInboxOutOfRangeIPwithDisabledCheckTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        configurationService.setProperty("ldn.notify.inbox.block-untrusted-ip", false);
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
                                .withLowerIp("127.0.0.1")
                                .withUpperIp("127.0.0.1")
                                .build();
        context.restoreAuthSystemState();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        String message = announceEndorsement.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);

        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox").with(remoteHost("mydocker.url", "172.23.0.1"))
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());

        LDNMessageEntity ldnMessage = ldnMessageService.find(context, notification.getId());
        checkStoredLDNMessage(notification, ldnMessage, object);
        assertEquals(ldnMessage.getQueueStatus(), LDNMessageEntity.QUEUE_STATUS_UNTRUSTED_IP);
    }

    private static RequestPostProcessor remoteHost(final String remoteHost, final String remoteAddr) {
        return request -> {
            request.setRemoteHost(remoteHost);
            request.setRemoteAddr(remoteAddr);
            return request;
        };
    }

    @Override
    @After
    public void destroy() throws Exception {
        List<LDNMessageEntity> ldnMessageEntities = ldnMessageService.findAll(context);
        if (CollectionUtils.isNotEmpty(ldnMessageEntities)) {
            ldnMessageEntities.forEach(ldnMessage -> {
                try {
                    ldnMessageService.delete(context, ldnMessage);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        super.destroy();
    }
}