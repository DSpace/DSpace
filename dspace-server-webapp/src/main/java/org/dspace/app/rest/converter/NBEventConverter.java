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
import org.dspace.app.nbevent.service.dto.NBMessageDTO;
import org.dspace.app.nbevent.service.dto.OpenaireMessageDTO;
import org.dspace.app.rest.model.NBEventMessageRest;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.model.OpenaireNBEventMessageRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.NBEvent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link NBEvent} to
 * {@link NBEventRest}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
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
            rest.setMessage(convertMessage(jsonMapper.readValue(modelObject.getMessage(),
                modelObject.getMessageDtoClass())));
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

    private NBEventMessageRest convertMessage(NBMessageDTO dto) {
        if (dto instanceof OpenaireMessageDTO) {
            OpenaireMessageDTO openaireDto = (OpenaireMessageDTO) dto;
            OpenaireNBEventMessageRest message = new OpenaireNBEventMessageRest();
            message.setAbstractValue(openaireDto.getAbstracts());
            message.setOpenaireId(openaireDto.getOpenaireId());
            message.setAcronym(openaireDto.getAcronym());
            message.setCode(openaireDto.getCode());
            message.setFunder(openaireDto.getFunder());
            message.setFundingProgram(openaireDto.getFundingProgram());
            message.setJurisdiction(openaireDto.getJurisdiction());
            message.setTitle(openaireDto.getTitle());
            message.setType(openaireDto.getType());
            message.setValue(openaireDto.getValue());
            return message;
        }

        throw new IllegalArgumentException("Unknown message type: " + dto.getClass());
    }

    @Override
    public Class<NBEvent> getModelClass() {
        return NBEvent.class;
    }

}
