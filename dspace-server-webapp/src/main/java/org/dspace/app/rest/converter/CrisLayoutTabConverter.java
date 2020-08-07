/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.LayoutSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutTab to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabConverter implements DSpaceConverter<CrisLayoutTab, CrisLayoutTabRest> {

    @Autowired
    private EntityTypeService eService;

    @Autowired
    private CrisLayoutBoxConverter boxConverter;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert
     * (java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public CrisLayoutTabRest convert(CrisLayoutTab mo, Projection projection) {
        CrisLayoutTabRest rest = new CrisLayoutTabRest();
        rest.setId(mo.getID());
        rest.setEntityType(mo.getEntity().getLabel());
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

    public CrisLayoutTab toModel(Context context, CrisLayoutTabRest rest) {
        EntityType eType = null;
        try {
            eType = eService.findByEntityType(context, rest.getEntityType());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        CrisLayoutTab tab = new CrisLayoutTab();
        tab.setHeader(rest.getHeader());
        tab.setPriority(rest.getPriority());
        tab.setSecurity(LayoutSecurity.valueOf(rest.getSecurity()));
        tab.setShortName(rest.getShortname());
        tab.setEntity(eType);
        if (rest.getBoxes() != null && rest.getBoxes().size() > 0) {
            List<CrisLayoutBox> boxes = new ArrayList<>();
            for (CrisLayoutBoxRest boxRest: rest.getBoxes()) {
                tab.addBox( boxConverter.toModel(context, boxRest) );
            }
        }
        return tab;
    }
}
