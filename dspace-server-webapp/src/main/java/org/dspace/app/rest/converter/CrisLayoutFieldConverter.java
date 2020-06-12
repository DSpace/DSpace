/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutField to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutFieldConverter implements DSpaceConverter<CrisLayoutField, CrisLayoutFieldRest> {

    @Override
    public CrisLayoutFieldRest convert(CrisLayoutField mo, Projection projection) {
        CrisLayoutFieldRest rest = new CrisLayoutFieldRest();
        rest.setBundle(mo.getBundle());
        rest.setId(mo.getID());
        rest.setLabel(mo.getLabel());
        rest.setPriority(mo.getPriority());
        rest.setRendering(mo.getRendering());
        rest.setRow(mo.getRow());
        rest.setStyle(mo.getStyle());
        return rest;
    }

    @Override
    public Class<CrisLayoutField> getModelClass() {
        return CrisLayoutField.class;
    }

    public CrisLayoutField toModel(Context context, CrisLayoutFieldRest rest) {
        CrisLayoutField field = new CrisLayoutField();
        field.setId(rest.getId());
        field.setBundle(rest.getBundle());
        field.setLabel(rest.getLabel());
        field.setPriority(rest.getPriority());
        field.setRendering(rest.getRendering());
        field.setRow(rest.getRow());
        field.setStyle(rest.getStyle());
        field.setType(rest.getType());
        return field;
    }
}
