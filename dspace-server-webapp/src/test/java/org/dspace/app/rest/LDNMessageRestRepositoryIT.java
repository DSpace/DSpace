/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_PATCH_JSON;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
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
import org.dspace.app.rest.model.LDNMessageEntityRest;
import org.dspace.app.rest.repository.LDNMessageRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Tests against {@link LDNMessageRestRepository}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LDNMessageService ldnMessageService;

    @Test
    public void findOneUnAuthorizedTest() throws Exception {
        getClient()
            .perform(get("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneIsForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneIsNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("item title")
                               .build();

        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
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

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/messages/" + notification.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(notification.getId())))
            .andExpect(jsonPath("$.notificationId", is(notification.getId())))
            .andExpect(jsonPath("$.queueStatus", is(1)))
            .andExpect(jsonPath("$.queueStatusLabel", is("QUEUE_STATUS_QUEUED")))
            .andExpect(jsonPath("$.context", is(item.getID().toString())))
            .andExpect(jsonPath("$.object", is(item.getID().toString())))
            .andExpect(jsonPath("$.target", nullValue()))
            .andExpect(jsonPath("$.origin", is(notifyService.getID())))
            .andExpect(jsonPath("$.inReplyTo", nullValue()))
            .andExpect(jsonPath("$.activityStreamType", is("Announce")))
            .andExpect(jsonPath("$.coarNotifyType", is("coar-notify:EndorsementAction")))
            .andExpect(jsonPath("$.queueAttempts", is(0)))
            .andExpect(jsonPath("$.notificationType", is("Incoming")));
    }

    @Test
    public void findAllUnAuthorizedTest() throws Exception {
        getClient().perform(get("/api/ldn/messages"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllIsForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/ldn/messages"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection)
                               .withTitle("item title")
                               .build();

        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("service url")
                                .withLdnUrl("https://overlay-journal.com/inbox/")
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

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/ldn/messages/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.messages[0].id", is(notification.getId())))
            .andExpect(jsonPath("$._embedded.messages[0].notificationId", is(notification.getId())))
            .andExpect(jsonPath("$._embedded.messages[0].queueStatus", is(1)))
            .andExpect(jsonPath("$._embedded.messages[0].queueStatusLabel", is("QUEUE_STATUS_QUEUED")))
            .andExpect(jsonPath("$._embedded.messages[0].context", is(item.getID().toString())))
            .andExpect(jsonPath("$._embedded.messages[0].object", is(item.getID().toString())))
            .andExpect(jsonPath("$._embedded.messages[0].target", nullValue()))
            .andExpect(jsonPath("$._embedded.messages[0].origin", is(notifyService.getID())))
            .andExpect(jsonPath("$._embedded.messages[0].inReplyTo", nullValue()))
            .andExpect(jsonPath("$._embedded.messages[0].activityStreamType", is("Announce")))
            .andExpect(jsonPath("$._embedded.messages[0].coarNotifyType", is("coar-notify:EndorsementAction")))
            .andExpect(jsonPath("$._embedded.messages[0].queueAttempts", is(0)))
            .andExpect(jsonPath("$._embedded.messages[0].notificationType", is("Incoming")));
    }

    @Test
    public void createLDNMessageTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        LDNMessageEntityRest data = new LDNMessageEntityRest();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken)
            .perform(post("/api/ldn/messages")
                .content(mapper.writeValueAsBytes(data))
                .contentType(contentType))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void deleteLDNMessageTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(delete("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void patchLDNMessageTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(patch("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4")
                .content(getPatchContent(List.of()))
                .contentType(APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isMethodNotAllowed());
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
