/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.AInprogressSubmissionRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract implementation providing the common functionalities for all the inprogressSubmission Converter
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <T>
 *            the DSpace API inprogressSubmission object
 * @param <R>
 *            the DSpace REST inprogressSubmission representation
 * @param <ID>
 *            the Serializable class used as primary key
 */
public abstract class AInprogressItemConverter<T extends InProgressSubmission<ID>,
                            R extends AInprogressSubmissionRest<ID>, ID extends Serializable>
        implements IndexableObjectConverter<T, R> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AInprogressItemConverter.class);

    @Autowired
    private EPersonConverter epersonConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    protected SubmissionConfigReader submissionConfigReader;

    @Autowired
    private SubmissionDefinitionConverter submissionDefinitionConverter;

    @Autowired
    private SubmissionSectionConverter submissionSectionConverter;

    @Autowired
    SubmissionService submissionService;

    public AInprogressItemConverter() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    protected void fillFromModel(T obj, R witem) {
        Collection collection = obj.getCollection();
        Item item = obj.getItem();
        EPerson submitter = null;
        try {
            submitter = obj.getSubmitter();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        witem.setId(obj.getID());
        witem.setCollection(collection != null ? collectionConverter.convert(collection) : null);
        witem.setItem(itemConverter.convert(item));
        witem.setSubmitter(epersonConverter.convert(submitter));

        // 1. retrieve the submission definition
        // 2. iterate over the submission section to allow to plugin additional
        // info

        if (collection != null) {
            SubmissionDefinitionRest def = submissionDefinitionConverter
                .convert(submissionConfigReader.getSubmissionConfigByCollection(collection.getHandle()));
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

                    if (stepInstance instanceof AbstractRestProcessingStep) {
                        // load the interface for this step
                        AbstractRestProcessingStep stepProcessing =
                            (AbstractRestProcessingStep) stepClass.newInstance();
                        for (ErrorRest error : stepProcessing.validate(submissionService, obj, stepConfig)) {
                            addError(witem.getErrors(), error);
                        }
                        witem.getSections()
                            .put(sections.getId(), stepProcessing.getData(submissionService, obj, stepConfig));
                    } else {
                        log.warn("The submission step class specified by '" + stepConfig.getProcessingClassName() +
                                 "' does not extend the class org.dspace.app.rest.submit.AbstractRestProcessingStep!" +
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