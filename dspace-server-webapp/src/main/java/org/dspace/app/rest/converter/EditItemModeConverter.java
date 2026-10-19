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
 * Converter that transforms {@link EditItemMode} domain objects into {@link EditItemModeRest}
 * REST representations.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This converter is responsible for transforming EditItemMode configuration objects into
 * their REST API representations. EditItemModes define role-based editing experiences for
 * already-published items, controlling which metadata fields are visible and editable based
 * on the user's role (e.g., FULL access for administrators, OWNER access for profile owners).</p>
 *
 * <p><strong>What EditItemMode Represents:</strong></p>
 * <p>An EditItemMode is a configuration object that defines:</p>
 * <ul>
 *   <li><strong>Name:</strong> Unique identifier for the mode (e.g., "FULL", "OWNER", "INVESTIGATOR")</li>
 *   <li><strong>Label:</strong> Human-readable display name for the UI</li>
 *   <li><strong>Submission Definition:</strong> Which submission configuration to use for editing
 *       (reuses submission infrastructure for consistent UI)</li>
 *   <li><strong>Security Constraints:</strong> Who can use this mode (not included in REST representation)</li>
 * </ul>
 *
 * <p><strong>Usage in REST Workflow:</strong></p>
 * <ol>
 *   <li>User requests available edit modes for an item</li>
 *   <li>{@link org.dspace.app.rest.repository.EditItemModeRestRepository} retrieves applicable modes</li>
 *   <li>This converter transforms each mode to REST format</li>
 *   <li>UI displays mode options to the user (e.g., "Full Edit" vs "Profile Owner Edit")</li>
 *   <li>User selects a mode and begins editing via {@link org.dspace.content.edit.EditItem}</li>
 * </ol>
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItemMode
 * @see EditItemModeRest
 * @see org.dspace.app.rest.repository.EditItemModeRestRepository
 * @see EditItemConverter
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
