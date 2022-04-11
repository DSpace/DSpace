/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.AInprogressSubmissionRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.DataProcessingStep;
import org.dspace.app.rest.submit.RestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Abstract implementation providing the common functionalities for all the inprogressSubmission Converter
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <T>
 *            the DSpace API inprogressSubmission object
 * @param <R>
 *            the DSpace REST inprogressSubmission representation
 */
public abstract class AInprogressItemConverter<T extends InProgressSubmission,
                            R extends AInprogressSubmissionRest>
        implements IndexableObjectConverter<T, R> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AInprogressItemConverter.class);

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Autowired
    private SubmissionSectionConverter submissionSectionConverter;

    protected SubmissionConfigReader submissionConfigReader;

    @Autowired
    SubmissionService submissionService;

    public AInprogressItemConverter() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    protected void fillFromModel(T obj, R witem, Projection projection) {
        Collection collection = obj.getCollection();
        Item item = obj.getItem();
        EPerson submitter = null;
        submitter = obj.getSubmitter();

        witem.setId(obj.getID());
        witem.setCollection(collection != null ? converter.toRest(collection, projection) : null);
        witem.setItem(converter.toRest(item, projection));
        if (submitter != null) {
            witem.setSubmitter(converter.toRest(submitter, projection));
        }

        // 1. retrieve the submission definition
        // 2. iterate over the submission section to allow to plugin additional
        // info

        if (collection != null) {
            SubmissionDefinitionRest def = converter.toRest(
                    submissionConfigReader.getSubmissionConfigByCollection(collection.getHandle()), projection);
            witem.setSubmissionDefinition(def);
            for (SubmissionSectionRest sections : def.getPanels()) {
                SubmissionStepConfig stepConfig = submissionSectionConverter.toModel(sections);

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
                        DataProcessingStep stepProcessing =
                            (DataProcessingStep) stepClass.newInstance();
                        for (ErrorRest error : stepProcessing.validate(submissionService, obj, stepConfig)) {
                            addError(witem.getErrors(), error);
                        }
                        witem.getSections()
                            .put(sections.getId(), stepProcessing.getData(submissionService, obj, stepConfig));
                    } else if (!(stepInstance instanceof RestProcessingStep)) {
                        log.warn("The submission step class specified by '" + stepConfig.getProcessingClassName() +
                                 "' does not implement the interface org.dspace.app.rest.submit.RestProcessingStep!" +
                                 " Therefore it cannot be used by the Configurable Submission as the " +
                                 "<processing-class>!");
                    }

                } catch (Exception e) {
                    log.error("An error occurred during the unmarshal of the data for the section " + sections.getId()
                            + " - reported error: " + e.getMessage(), e);
                }

            }
        }
    }

    private void addError(List<ErrorRest> errors, ErrorRest toAdd) {

        boolean found = false;
        String i18nKey = toAdd.getMessage();
        if (StringUtils.isNotBlank(i18nKey)) {
            for (ErrorRest error : errors) {
                if (i18nKey.equals(error.getMessage())) {
                    error.getPaths().addAll(toAdd.getPaths());
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            errors.add(toAdd);
        }
    }

}
