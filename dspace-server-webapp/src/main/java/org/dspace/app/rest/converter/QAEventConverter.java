/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import org.dspace.app.rest.model.CorrectionTypeQAEventMessageRest;
import org.dspace.app.rest.model.NotifyQAEventMessageRest;
import org.dspace.app.rest.model.OpenaireQAEventMessageRest;
import org.dspace.app.rest.model.QAEventMessageRest;
import org.dspace.app.rest.model.QAEventRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.QAEvent;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.qaevent.service.dto.NotifyMessageDTO;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link QAEvent} to
 * {@link QAEventRest}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class QAEventConverter implements DSpaceConverter<QAEvent, QAEventRest> {

    private static final String OPENAIRE_PID_HREF_PREFIX_PROPERTY = "qaevents.openaire.pid-href-prefix.";

    @Autowired
    private ConfigurationService configurationService;

    private ObjectMapper jsonMapper;

    @PostConstruct
    public void setup() {
        jsonMapper = new JsonMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public QAEventRest convert(QAEvent modelObject, Projection projection) {
        QAEventRest rest = new QAEventRest();
        rest.setId(modelObject.getEventId());
        try {
            rest.setMessage(convertMessage(jsonMapper.readValue(modelObject.getMessage(),
                                                                modelObject.getMessageDtoClass())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        rest.setSource(modelObject.getSource());
        rest.setOriginalId(modelObject.getOriginalId());
        rest.setProjection(projection);
        rest.setTitle(modelObject.getTitle());
        rest.setTopic(modelObject.getTopic());
        rest.setEventDate(modelObject.getLastUpdate());
        DecimalFormat decimalFormat = new DecimalFormat("0.000", new DecimalFormatSymbols(Locale.ENGLISH));
        rest.setTrust(decimalFormat.format(modelObject.getTrust()));
        // right now only the pending status can be found in persisted qa events
        rest.setStatus(modelObject.getStatus());
        return rest;
    }

    private QAEventMessageRest convertMessage(QAMessageDTO dto) {
        if (dto instanceof OpenaireMessageDTO) {
            return convertOpenaireMessage(dto);
        } else if (dto instanceof NotifyMessageDTO) {
            return convertNotifyMessage(dto);
        }
        if (dto instanceof CorrectionTypeMessageDTO) {
            return convertCorrectionTypeMessage(dto);
        }
        throw new IllegalArgumentException("Unknown message type: " + dto.getClass());
    }

    private QAEventMessageRest convertNotifyMessage(QAMessageDTO dto) {
        NotifyMessageDTO notifyDto = (NotifyMessageDTO) dto;
        NotifyQAEventMessageRest message = new NotifyQAEventMessageRest();
        message.setServiceName(notifyDto.getServiceName());
        message.setServiceId(notifyDto.getServiceId());
        message.setHref(notifyDto.getHref());
        message.setRelationship(notifyDto.getRelationship());
        return message;
    }

    private QAEventMessageRest convertCorrectionTypeMessage(QAMessageDTO dto) {
        CorrectionTypeMessageDTO correctionTypeDto = (CorrectionTypeMessageDTO) dto;
        CorrectionTypeQAEventMessageRest message = new CorrectionTypeQAEventMessageRest();
        message.setReason(correctionTypeDto.getReason());
        return message;
    }

    private QAEventMessageRest convertOpenaireMessage(QAMessageDTO dto) {
        OpenaireMessageDTO openaireDto = (OpenaireMessageDTO) dto;
        OpenaireQAEventMessageRest message = new OpenaireQAEventMessageRest();
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
        message.setPidHref(calculateOpenairePidHref(openaireDto.getType(), openaireDto.getValue()));
        return message;
    }

    private String calculateOpenairePidHref(String type, String value) {
        if (type == null) {
            return null;
        }

        String hrefType = type.toLowerCase();
        if (!configurationService.hasProperty(OPENAIRE_PID_HREF_PREFIX_PROPERTY + hrefType)) {
            return null;
        }

        String hrefPrefix = configurationService.getProperty(OPENAIRE_PID_HREF_PREFIX_PROPERTY + hrefType, "");
        return hrefPrefix + value;
    }

    @Override
    public Class<QAEvent> getModelClass() {
        return QAEvent.class;
    }

}
