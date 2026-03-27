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

    /**
     * Converts an EditItemMode domain model object to its REST representation.
     * <p>
     * This method transforms the EditItemMode object into an EditItemModeRest object,
     * mapping the mode's name, label, and associated submission definition to the
     * corresponding REST resource fields.
     *
     * @param model      the EditItemMode domain object to convert
     * @param projection the projection object (currently unused in this conversion)
     * @return the EditItemModeRest representation of the input model
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

    /**
     * Returns the class type of the domain model handled by this converter.
     * <p>
     * This method is used by the DSpace REST framework to identify which
     * domain model class this converter is responsible for transforming.
     *
     * @return the Class object representing EditItemMode
     */
    @Override
    public Class<EditItemMode> getModelClass() {
        return EditItemMode.class;
    }

}
