/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.converter;

import java.util.List;

import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.springframework.stereotype.Component;


/**
 * This is the converter from/to the Linkset in the DSpace API data model and the REST data model.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
@Component
public class LinksetConverter implements DSpaceConverter<List<Linkset>, LinksetRest> {

    @Override
    public LinksetRest convert(List<Linkset> linksets, Projection projection) {
        LinksetRest linksetRest = new LinksetRest();
        linksetRest.setProjection(projection);
        linksetRest.setLinkset(linksets);
        return linksetRest;
    }

    @Override
    public Class<List<Linkset>> getModelClass() {
        return (Class<List<Linkset>>) ((Class) List.class);
    }
}
