/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutBox to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxConverter implements DSpaceConverter<CrisLayoutBox, CrisLayoutBoxRest> {

    @Autowired
    private EntityTypeService eService;

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
        rest.setHeader(mo.getHeader());
        rest.setId(mo.getID());
        rest.setMinor(mo.getMinor());
        rest.setSecurity(mo.getSecurity());
        rest.setShortname(mo.getShortname());
        rest.setStyle(mo.getStyle());
        rest.setClear(mo.getClear());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<CrisLayoutBox> getModelClass() {
        return CrisLayoutBox.class;
    }

    public CrisLayoutBox toModel(Context context, CrisLayoutBoxRest rest) {
        EntityType eType = null;
        try {
            eType = eService.findByEntityType(context, rest.getEntityType());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        CrisLayoutBox box = new CrisLayoutBox();
        box.setEntitytype(eType);
        box.setType(rest.getBoxType());
        box.setCollapsed(rest.getCollapsed());
        box.setHeader(rest.getHeader());
        box.setId(rest.getId());
        box.setMinor(rest.getMinor());
        box.setSecurity(LayoutSecurity.valueOf(rest.getSecurity()));
        box.setShortname(rest.getShortname());
        box.setStyle(rest.getStyle());
        box.setClear(rest.getClear());
        return box;
    }
}
