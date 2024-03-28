/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
 * Integration Tests against {@link LDNMessageRestController}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private LDNMessageService ldnMessageService;

    @Test
    public void findByItemUnAuthorizedTest() throws Exception {
        getClient()
                .perform(post("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4/enqueueretry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByItemIsForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
            .perform(post("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4/enqueueretry"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findByItemNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(post("/api/ldn/messages/urn:uuid:668f26e0-2c8d-4117-a0d2-ee713523bcb4/enqueueretry"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findByItemTest() throws Exception {
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
            .perform(post("/api/ldn/messages/" + notification.getId() + "/enqueueretry"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(notification.getId())))
            .andExpect(jsonPath("$.notificationId", is(notification.getId())))
            .andExpect(jsonPath("$.queueStatus", is(7)))
            .andExpect(jsonPath("$.queueStatusLabel", is("QUEUE_STATUS_QUEUED_FOR_RETRY")))
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