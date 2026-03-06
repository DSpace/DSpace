/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.EditItemMode;
import org.springframework.stereotype.Component;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class EditItemModeConverter implements DSpaceConverter<EditItemMode, EditItemModeRest> {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#
     * convert(java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public EditItemModeRest convert(EditItemMode model, Projection projection) {
        EditItemModeRest rest = new EditItemModeRest();
        rest.setId(model.getName());
        rest.setName(model.getName());
        rest.setLabel(model.getLabel());
        rest.setSubmissionDefinition(model.getSubmissionDefinition());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<EditItemMode> getModelClass() {
        return EditItemMode.class;
    }

}
