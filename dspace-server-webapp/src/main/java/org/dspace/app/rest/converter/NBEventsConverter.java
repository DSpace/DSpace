/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.nbevent.service.dto.NBEventQueryDto;
import org.dspace.app.rest.model.NBEventMessage;
import org.dspace.app.rest.model.NBEventRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

@Component
public class NBEventsConverter implements DSpaceConverter<NBEventQueryDto, NBEventRest> {

	@Override
	public NBEventRest convert(NBEventQueryDto modelObject, Projection projection) {
		NBEventRest rest = new NBEventRest();
		rest.setId(modelObject.getResourceUUID());
		rest.setMessage(new NBEventMessage(modelObject.getMessage()));
		rest.setOriginalId(modelObject.getOriginalId());
		rest.setProjection(projection);
		rest.setTitle(modelObject.getTitle());
		rest.setTopic(modelObject.getTopic());
		rest.setTrust(modelObject.getTrust());
		return rest;
	}

	@Override
	public Class<NBEventQueryDto> getModelClass() {
		return NBEventQueryDto.class;
	}

}
