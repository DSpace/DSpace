/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.impl;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link QAEventActionService}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventActionServiceImpl implements QAEventActionService {

    private static final Logger log = LogManager.getLogger(QAEventActionServiceImpl.class);

    private ObjectMapper jsonMapper;

    @Autowired
    private QAEventService qaEventService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    private Map<String, QualityAssuranceAction> topicsToActions;

    public void setTopicsToActions(Map<String, QualityAssuranceAction> topicsToActions) {
        this.topicsToActions = topicsToActions;
    }

    public Map<String, QualityAssuranceAction> getTopicsToActions() {
        return topicsToActions;
    }

    public QAEventActionServiceImpl() {
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void accept(Context context, QAEvent qaevent) {
        Item item = null;
        Item related = null;
        try {
            item = itemService.find(context, UUID.fromString(qaevent.getTarget()));
            if (qaevent.getRelated() != null) {
                related = itemService.find(context, UUID.fromString(qaevent.getRelated()));
            }
            if (topicsToActions.get(qaevent.getTopic()) == null) {
                String msg = "Unable to manage QA Event typed " + qaevent.getTopic()
                    + ". Managed types are: " + topicsToActions;
                log.error(msg);
                throw new RuntimeException(msg);
            }
            context.turnOffAuthorisationSystem();
            topicsToActions.get(qaevent.getTopic()).applyCorrection(context, item, related,
                jsonMapper.readValue(qaevent.getMessage(), qaevent.getMessageDtoClass()));
            qaEventService.deleteEventByEventId(qaevent.getEventId());
            makeAcknowledgement(qaevent.getEventId(), qaevent.getSource(), QAEvent.ACCEPTED);
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public void discard(Context context, QAEvent qaevent) {
        qaEventService.deleteEventByEventId(qaevent.getEventId());
        makeAcknowledgement(qaevent.getEventId(), qaevent.getSource(), QAEvent.DISCARDED);
    }

    @Override
    public void reject(Context context, QAEvent qaevent) {
        qaEventService.deleteEventByEventId(qaevent.getEventId());
        makeAcknowledgement(qaevent.getEventId(), qaevent.getSource(), QAEvent.REJECTED);
    }

    /**
     * Make acknowledgement to the configured urls for the event status.
     */
    private void makeAcknowledgement(String eventId, String source, String status) {
        String[] ackwnoledgeCallbacks = configurationService
            .getArrayProperty("qaevents." + source + ".acknowledge-url");
        if (ackwnoledgeCallbacks != null) {
            for (String ackwnoledgeCallback : ackwnoledgeCallbacks) {
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
    }
}