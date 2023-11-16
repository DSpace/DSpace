/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.content.QAEvent.COAR_NOTIFY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class LDNInboxControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LDNMessageService ldnMessageService;

    private QAEventService qaEventService = new DSpace().getSingletonService(QAEventService.class);

    @Test
    public void ldnInboxEndorsementActionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        context.restoreAuthSystemState();

        InputStream offerEndorsementStream = getClass().getResourceAsStream("ldn_offer_endorsement_object.json");
        String offerEndorsementJson = IOUtils.toString(offerEndorsementStream, Charset.defaultCharset());
        offerEndorsementStream.close();
        String message = offerEndorsementJson.replace("<<object>>", object);
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
    public void ldnInboxAnnounceEndorsementTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();

        context.restoreAuthSystemState();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        String message = announceEndorsement.replace("<<object>>", object);

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
        NotifyServiceEntity serviceEntity = NotifyServiceBuilder.createNotifyServiceBuilder(context)
                .withName("Review Service")
                .withLdnUrl("https://review-service.com/inbox/")
                .withScore(BigDecimal.valueOf(0.6d))
                .build();
        Community com = CommunityBuilder.createCommunity(context).withName("Test Community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("Test Collection").build();
        Item item = ItemBuilder.createItem(context, col).withHandle("123456789/9999").withTitle("Test Item").build();
        context.restoreAuthSystemState();
        InputStream announceReviewStream = getClass().getResourceAsStream("ldn_announce_review.json");
        String message = IOUtils.toString(announceReviewStream, Charset.defaultCharset());
        announceReviewStream.close();
        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(message, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());
        assertThat(qaEventService.findAllSources(context, 0, 20), hasItem(QASourceMatcher.with(COAR_NOTIFY, 1L)));

        assertThat(qaEventService.findAllTopicsBySource(context, COAR_NOTIFY, 0, 20), contains(
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

}