/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.layout.CrisLayoutBox;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component
public class CrisLayoutBoxConverter implements DSpaceConverter<CrisLayoutBox, CrisLayoutBoxRest> {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert
     * (java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public CrisLayoutBoxRest convert(CrisLayoutBox mo, Projection projection) {
        CrisLayoutBoxRest rest = new CrisLayoutBoxRest();
        rest.setBoxType(mo.getType());
        rest.setCollapsed(mo.getCollapsed());
        rest.setEntityType(mo.getEntitytype().getLabel());
        rest.setGroup(-1); // TODO cosa devo inserire in questo capo?
        rest.setHeader(mo.getHeader());
        rest.setId(mo.getID());
        rest.setMinor(mo.getMinor());
        rest.setPriority(mo.getPriority());
        rest.setSecurity(mo.getSecurity());
        rest.setShortname(mo.getShortname());
        rest.setStyle(mo.getStyle());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<CrisLayoutBox> getModelClass() {
        return CrisLayoutBox.class;
    }

}
