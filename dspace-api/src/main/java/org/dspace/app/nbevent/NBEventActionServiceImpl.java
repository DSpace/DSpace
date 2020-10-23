/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class NBEventActionServiceImpl implements NBEventActionService {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(NBEventActionServiceImpl.class);

    private ObjectMapper jsonMapper;

    @Autowired
    private NBEventService nbEventService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    private Map<String, NBAction> topicsToActions;

    public void setTopicsToActions(Map<String, NBAction> topicsToActions) {
        this.topicsToActions = topicsToActions;
    }

    public Map<String, NBAction> getTopicsToActions() {
        return topicsToActions;
    }

    public NBEventActionServiceImpl() {
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void accept(Context context, NBEvent nbevent) {
        Item item = null;
        Item related = null;
        try {
            item = itemService.find(context, UUID.fromString(nbevent.getTarget()));
            if (nbevent.getRelated() != null) {
                related = itemService.find(context, UUID.fromString(nbevent.getRelated()));
            }
            topicsToActions.get(nbevent.getTopic()).applyCorrection(context, item, related,
                    jsonMapper.readValue(nbevent.getMessage(), MessageDto.class));
            nbEventService.deleteEventByEventId(context, nbevent.getEventId());
            makeAcknowledgement(nbevent.getEventId(), NBEvent.ACCEPTED);
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void discard(Context context, NBEvent nbevent) {
        nbEventService.deleteEventByEventId(context, nbevent.getEventId());
        makeAcknowledgement(nbevent.getEventId(), NBEvent.DISCARDED);
    }

    @Override
    public void reject(Context context, NBEvent nbevent) {
        nbEventService.deleteEventByEventId(context, nbevent.getEventId());
        makeAcknowledgement(nbevent.getEventId(), NBEvent.REJECTED);
    }

    private void makeAcknowledgement(String eventId, String status) {
        String ackwnoledgeCallback = configurationService.getProperty("acknowledge-url");
        if (StringUtils.isNotBlank(ackwnoledgeCallback)) {
            ObjectNode node = jsonMapper.createObjectNode();
            node.put("eventId", eventId);
            node.put("status", status);
            StringEntity requestEntity = new StringEntity(node.toString(), ContentType.APPLICATION_JSON);
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost postMethod = new HttpPost(ackwnoledgeCallback);
            postMethod.setEntity(requestEntity);
            try {
                httpclient.execute(postMethod);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}