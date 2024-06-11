/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.app.rest.model.NotifyRequestStatusRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Rest Controller for NotifyRequestStatus targeting items IT
 * class {@link NotifyRequestStatusRestController}
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
public class NotifyRequestStatusRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LDNMessageService ldnMessageService;

    @Test
    public void oneStatusReviewedTest() throws Exception {
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
                                .withStatus(true)
                                .withScore(BigDecimal.valueOf(0.6d))
                                .build();
        //SEND OFFER REVIEW
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

        //CHECK THE SERVICE ON ITS notifystatus ARRAY
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/" + NotifyRequestStatusRest.CATEGORY + "/"
                + NotifyRequestStatusRest.NAME + "/" + item.getID())
                .contentType("application/ld+json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyStatus").isArray())
            .andExpect(jsonPath("$.notifyStatus").isNotEmpty())
            .andExpect(jsonPath("$.notifyStatus[0].status").value("REQUESTED"))
            .andExpect(jsonPath("$.notifyStatus[0].serviceUrl").value("https://review-service.com/inbox/about/"))
            ;
    }

    @Test
    public void oneStatusAnnounceEndorsementTestDisabledService() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();
        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();
        NotifyServiceEntity notifyServiceEntity = NotifyServiceBuilder
                                .createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://review-service.com/inbox/about/")
                                .withLdnUrl("https://review-service.com/inbox/")
                                .withStatus(false) // service is disabled
                                .withScore(BigDecimal.valueOf(0.6d))
                                .build();
        //SEND OFFER REVIEW
        InputStream offerReviewStream = getClass().getResourceAsStream("ldn_offer_review3.json");
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
        assertEquals(processed, 0);
        processed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(processed, 0);

    }

    @Test
    public void oneStatusRejectedTest() throws Exception {
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
                                .withStatus(true)
                                .withScore(BigDecimal.valueOf(0.6d))
                                .build();
        //SEND OFFER REVIEW
        InputStream offerReviewStream = getClass().getResourceAsStream("ldn_offer_review2.json");
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
        //SEND ACK REVIEW REJECTED
        InputStream ackReviewStream = getClass().getResourceAsStream("ldn_ack_review_reject.json");
        String ackReview = IOUtils.toString(ackReviewStream, Charset.defaultCharset());
        ackReviewStream.close();
        String ackMessage = ackReview.replaceAll("<<object_handle>>", object);
        ackMessage = ackMessage.replaceAll(
            "<<ldn_offer_review_uuid>>", "urn:uuid:0370c0fb-bb78-4a9b-87f5-bed307a509df");

        ObjectMapper ackMapper = new ObjectMapper();
        Notification ackNotification = ackMapper.readValue(ackMessage, Notification.class);
        getClient()
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(ackMessage))
            .andExpect(status().isAccepted());

        int ackProcessed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(ackProcessed, 1);
        ackProcessed = ldnMessageService.extractAndProcessMessageFromQueue(context);
        assertEquals(ackProcessed, 0);

        //CHECK THE SERVICE ON ITS notifystatus ARRAY
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/" + NotifyRequestStatusRest.CATEGORY + "/"
                + NotifyRequestStatusRest.NAME + "/" + item.getID())
                .contentType("application/ld+json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notifyStatus").isArray())
            .andExpect(jsonPath("$.notifyStatus").isNotEmpty())
            .andExpect(jsonPath("$.notifyStatus[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.notifyStatus[0].serviceUrl").value("https://review-service.com/inbox/about/"))
            .andExpect(jsonPath("$.notifyStatus[0].offerType").value("Review"))
            ;
    }
}
