/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.layout.CrisLayoutTab;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component
public class CrisLayoutTabConverter implements DSpaceConverter<CrisLayoutTab, CrisLayoutTabRest> {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert
     * (java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public CrisLayoutTabRest convert(CrisLayoutTab mo, Projection projection) {
        CrisLayoutTabRest rest = new CrisLayoutTabRest();
        rest.setId(mo.getID());
        rest.setShortname(mo.getShortName());
        rest.setHeader(mo.getHeader());
        rest.setPriority(mo.getPriority());
        rest.setSecurity(mo.getSecurity());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<CrisLayoutTab> getModelClass() {
        return CrisLayoutTab.class;
    }

}
