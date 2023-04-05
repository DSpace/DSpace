/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.service.ItemService;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;

/**
 * Execute file required check validation
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class UploadValidation extends AbstractValidation {

    private static final String ERROR_VALIDATION_FILEREQUIRED = "error.validation.filerequired";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UploadValidation.class);

    private ItemService itemService;

    private UploadConfigurationService uploadConfigurationService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {
        //TODO MANAGE METADATA
        List<ErrorRest> errors = new ArrayList<>();
        UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(config.getId());
        if (uploadConfig.isRequired() && !itemService.hasUploadedFiles(obj.getItem())) {
            addError(errors, ERROR_VALIDATION_FILEREQUIRED,
                     "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/"
                         + config.getId());
        }
        return errors;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public UploadConfigurationService getUploadConfigurationService() {
        return uploadConfigurationService;
    }

    public void setUploadConfigurationService(UploadConfigurationService uploadConfigurationService) {
        this.uploadConfigurationService = uploadConfigurationService;
    }


}
