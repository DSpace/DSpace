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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.app.rest.model.NBEventMessageRest;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.NBEvent;
import org.springframework.stereotype.Component;

@Component
public class NBEventConverter implements DSpaceConverter<NBEvent, NBEventRest> {

    private ObjectMapper jsonMapper;

    public NBEventConverter() {
        super();
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public NBEventRest convert(NBEvent modelObject, Projection projection) {
        NBEventRest rest = new NBEventRest();
        rest.setId(modelObject.getEventId());
        try {
            rest.setMessage(convertMessage(jsonMapper.readValue(modelObject.getMessage(), MessageDto.class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        rest.setOriginalId(modelObject.getOriginalId());
        rest.setProjection(projection);
        rest.setTitle(modelObject.getTitle());
        rest.setTopic(modelObject.getTopic());
        rest.setEventDate(modelObject.getLastUpdate());
        rest.setTrust(new DecimalFormat("0.000").format(modelObject.getTrust()));
        // right now only the pending status can be found in persisted nb events
        rest.setStatus(modelObject.getStatus());
        return rest;
    }

    private NBEventMessageRest convertMessage(MessageDto dto) {
        NBEventMessageRest message = new NBEventMessageRest();
        message.setAbstractValue(dto.getAbstracts());
        message.setOpenaireId(dto.getOpenaireId());
        message.setAcronym(dto.getAcronym());
        message.setCode(dto.getCode());
        message.setFunder(dto.getFunder());
        message.setFundingProgram(dto.getFundingProgram());
        message.setJurisdiction(dto.getJurisdiction());
        message.setTitle(dto.getTitle());
        message.setType(dto.getType());
        message.setValue(dto.getValue());
        return message;
    }

    @Override
    public Class<NBEvent> getModelClass() {
        return NBEvent.class;
    }

}
