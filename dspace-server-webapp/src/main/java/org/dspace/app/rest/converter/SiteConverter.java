/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Site;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the community in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SiteConverter extends DSpaceObjectConverter<Site, SiteRest> {

    @Override
    public SiteRest convert(Site obj, Projection projection) {
        return super.convert(obj, projection);
    }

    @Override
    protected SiteRest newInstance() {
        return new SiteRest();
    }

    @Override
    public Class<Site> getModelClass() {
        return Site.class;
    }
}
