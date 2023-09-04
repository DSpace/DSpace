/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class LDNInboxControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

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

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());
    }

    @Test
    public void ldnInboxAnnounceEndorsementTest() throws Exception {

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String message = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();
        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(announceEndorsementStream, Notification.class);
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isAccepted());
    }

    @Test
    public void ldnInboxEndorsementActionBadRequestTest() throws Exception {
        // id is not an uri
        InputStream offerEndorsementStream = getClass().getResourceAsStream("ldn_offer_endorsement_badrequest.json");
        String message = IOUtils.toString(offerEndorsementStream, Charset.defaultCharset());
        offerEndorsementStream.close();
        ObjectMapper mapper = new ObjectMapper();
        Notification notification = mapper.readValue(offerEndorsementStream, Notification.class);
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(post("/ldn/inbox")
                .contentType("application/ld+json")
                .content(message))
            .andExpect(status().isBadRequest());
    }
}