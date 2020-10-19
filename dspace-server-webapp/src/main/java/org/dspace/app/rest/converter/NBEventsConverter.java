/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.app.rest.model.NBEventMessage;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.NBEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class NBEventsConverter implements DSpaceConverter<NBEvent, NBEventRest> {

    private ObjectMapper jsonMapper = new JsonMapper();

    private static final Logger logger = LoggerFactory.getLogger(NBEventsConverter.class);
    @Override
    public NBEventRest convert(NBEvent modelObject, Projection projection) {
        NBEventRest rest = new NBEventRest();
        rest.setId(modelObject.getTarget());
        try {
            rest.setMessage(new NBEventMessage(jsonMapper.readValue(modelObject.getMessage(), MessageDto.class)));
        } catch (JsonProcessingException e) {
            logger.error("Exception reading MessageDto", e);
        }
        rest.setOriginalId(modelObject.getOriginalId());
        rest.setProjection(projection);
        rest.setTitle(modelObject.getTitle());
        rest.setTopic(modelObject.getTopic());
        rest.setTrust(new DecimalFormat("#.000").format(modelObject.getTrust()));
        return rest;
    }

    @Override
    public Class<NBEvent> getModelClass() {
        return NBEvent.class;
    }

}
