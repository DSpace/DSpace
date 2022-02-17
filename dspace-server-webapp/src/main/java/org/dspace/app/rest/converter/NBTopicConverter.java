/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.nbevent.NBTopic;
import org.dspace.app.rest.model.NBTopicRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link NBTopic} to
 * {@link NBTopicRest}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class NBTopicConverter implements DSpaceConverter<NBTopic, NBTopicRest> {

    @Override
    public Class<NBTopic> getModelClass() {
        return NBTopic.class;
    }

    @Override
    public NBTopicRest convert(NBTopic modelObject, Projection projection) {
        NBTopicRest rest = new NBTopicRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getKey().replace("/", "!"));
        rest.setName(modelObject.getKey());
        rest.setLastEvent(modelObject.getLastEvent());
        rest.setTotalEvents(modelObject.getTotalEvents());
        return rest;
    }

}
