/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.DataProcessingStep;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.EditItemMode;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.validation.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Converter that transforms {@link EditItem} domain objects into {@link EditItemRest} REST representations.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This converter is responsible for transforming EditItem objects (which represent post-publication
 * item editing sessions) into their REST API representations. It extends
 * {@link AInprogressItemConverter} to reuse the submission infrastructure for editing already-published
 * items, providing a consistent editing experience whether creating new items or modifying existing ones.</p>
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItem
 * @see EditItemRest
 * @see EditItemMode
 * @see AInprogressItemConverter
 * @see org.dspace.app.rest.repository.EditItemRestRepository
 */
@Component
public class EditItemConverter
    extends AInprogressItemConverter<EditItem, EditItemRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EditItemConverter.class);

    @Autowired
    @Lazy
    private ConverterService converter;

    @Autowired
    private SubmissionSectionConverter submissionSectionConverter;

    @Autowired
    private ValidationService validationService;

    /**
     * Constructs a new EditItemConverter instance.
     * <p>
     * Initializes the converter by loading submission configuration settings
     * from the parent class. This constructor is called by Spring's dependency
     * injection framework.
     * </p>
     *
     * @throws SubmissionConfigReaderException if there is an error reading the submission configuration
     */
    public EditItemConverter() throws SubmissionConfigReaderException {
        super();
    }

    /**
     * Determines whether this converter supports the given indexable object.
     * <p>
     * This method checks if the indexed object contained within the provided
     * IndexableObject is an instance of EditItem, which this converter is
     * designed to handle.
     * </p>
     *
     * @param idxo the IndexableObject to check for compatibility
     * @return {@code true} if the indexed object is an EditItem instance, {@code false} otherwise
     */
    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof EditItem;
    }

    /**
     * Converts an EditItem domain model object to its REST representation.
     * <p>
     * This method creates a new EditItemRest instance and populates it with data
     * from the provided EditItem model object. The conversion respects the specified
     * projection to control which properties are included in the REST representation.
     * </p>
     *
     * @param modelObject the EditItem domain object to convert
     * @param projection  the projection to apply, controlling which properties are included in the output
     * @return the EditItemRest representation of the EditItem model object
     */
    @Override
    public EditItemRest convert(EditItem modelObject, Projection projection) {
        EditItemRest rest = new EditItemRest();
        rest.setProjection(projection);
        fillFromModel(modelObject, rest, projection);
        return rest;
    }

    /**
     * Populates the REST representation with data from the EditItem domain model.
     *
     * <p><strong>Population Process:</strong></p>
     * <ol>
     *   <li><strong>Extract Mode:</strong> Retrieves the {@link EditItemMode} which controls the editing behavior</li>
     *   <li><strong>Set Initial ID:</strong> Sets REST ID to "{itemUUID}:none" as a temporary placeholder</li>
     *   <li><strong>Process Mode (if present):</strong>
     *       <ul>
     *           <li>Adds validation errors to the REST object</li>
     *           <li>Updates REST ID to "{itemUUID}:{modeName}"</li>
     *           <li>Converts the mode's submission definition to REST format</li>
     *           <li>Stores the submission definition name for section processing</li>
     *           <li>Iterates through all submission sections</li>
     *       </ul>
     *   </li>
     *   <li><strong>Section Processing:</strong> For each submission section:
     *       <ul>
     *           <li>Converts the section from REST to model representation</li>
     *           <li>Checks if the section should be hidden for in-progress submissions</li>
     *           <li>Loads the section's processing class dynamically</li>
     *           <li>Invokes the {@link DataProcessingStep} to populate section-specific data</li>
     *           <li>Adds the populated section data to the REST object</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     *
     * @param obj        the EditItem domain object to convert from
     * @param rest       the EditItemRest object to populate
     * @param projection the projection controlling which properties to include
     */
    protected void fillFromModel(EditItem obj, EditItemRest rest, Projection projection) {
        EditItemMode mode = obj.getMode();

        rest.setId(obj.getID() + ":none");

        // 1. retrieve the submission definition
        // 2. iterate over the submission section to allow to plugin additional
        // info

        if (mode != null) {

            addValidationErrorsToItem(obj, rest);

            rest.setId(obj.getID() + ":" + mode.getName());
            SubmissionDefinitionRest def = converter.toRest(
                    submissionConfigService.getSubmissionConfigByName(mode.getSubmissionDefinition()), projection);
            rest.setSubmissionDefinition(def);
            storeSubmissionName(def.getName());
            for (SubmissionSectionRest sections : def.getPanels()) {
                SubmissionStepConfig stepConfig = submissionSectionConverter.toModel(sections);

                if (stepConfig.isHiddenForInProgressSubmission(obj)) {
                    continue;
                }

                /*
                 * First, load the step processing class (using the current
                 * class loader)
                 */
                ClassLoader loader = this.getClass().getClassLoader();
                Class stepClass;
                try {
                    stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                    Object stepInstance = stepClass.newInstance();

                    if (stepInstance instanceof DataProcessingStep) {
                        // load the interface for this step
                        DataProcessingStep stepProcessing = (DataProcessingStep) stepClass.newInstance();

                        rest.getSections()
                            .put(sections.getId(), stepProcessing.getData(submissionService, obj, stepConfig));
                    } else {
                        log.warn(
                            "The submission step class specified by '{}' " +
                            "does not extend the class org.dspace.app.rest.submit.AbstractRestProcessingStep! " +
                            "Therefore it cannot be used by the Configurable Submission as the <processing-class>!",
                            stepConfig.getProcessingClassName()
                        );
                    }

                } catch (Exception e) {
                    log.error(
                        "An error occurred during the unmarshal of the data for the section {} - reported error: {}",
                        sections.getId(), e.getMessage(), e
                    );
                }

            }
        }
    }

    private void addValidationErrorsToItem(EditItem obj, EditItemRest rest) {
        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());

        validationService.validate(context, obj).stream()
            .map(ErrorRest::fromValidationError)
            .forEach(error -> addError(rest.getErrors(), error));
    }

    /**
     * Returns the domain model class that this converter handles.
     * <p>
     * This method provides the Class object for EditItem, which is used by the
     * conversion framework to determine which converter to use for a given model object.
     * </p>
     *
     * @return the EditItem.class object representing the domain model class
     */
    @Override
    public Class<EditItem> getModelClass() {
        return EditItem.class;
    }
}
