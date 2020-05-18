package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutFieldRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.layout.CrisLayoutField;
import org.springframework.stereotype.Component;

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

}
