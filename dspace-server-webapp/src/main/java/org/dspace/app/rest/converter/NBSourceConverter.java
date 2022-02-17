/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.nbevent.NBSource;
import org.dspace.app.rest.model.NBSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link NBSource} to
 * {@link NBSourceRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class NBSourceConverter implements DSpaceConverter<NBSource, NBSourceRest> {

    @Override
    public Class<NBSource> getModelClass() {
        return NBSource.class;
    }

    @Override
    public NBSourceRest convert(NBSource modelObject, Projection projection) {
        NBSourceRest rest = new NBSourceRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getName());
        rest.setLastEvent(modelObject.getLastEvent());
        rest.setTotalEvents(modelObject.getTotalEvents());
        return rest;
    }

}
