/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.converter;

import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.signposting.model.Lset;
import org.springframework.stereotype.Component;


/**
 * This is the converter from/to the Lset in the DSpace API data model and the REST data model.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
@Component
public class LsetConverter implements DSpaceConverter<Lset, LinksetRest> {

    @Override
    public LinksetRest convert(Lset lset, Projection projection) {
        LinksetRest linksetRest = new LinksetRest();
        linksetRest.setProjection(projection);
        linksetRest.getLset().add(lset);
        return linksetRest;
    }

    @Override
    public Class<Lset> getModelClass() {
        return Lset.class;
    }
}
