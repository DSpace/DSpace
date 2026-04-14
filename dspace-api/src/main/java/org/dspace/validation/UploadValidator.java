/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.validation.model.ValidationError;

/**
 * Execute file required check validation
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4sciente.it)
 */
public class UploadValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_FILEREQUIRED = "error.validation.filerequired";

    private ItemService itemService;

    private UploadConfigurationService uploadConfigurationService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        //TODO MANAGE METADATA
        List<ValidationError> errors = new ArrayList<>();
        UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(config.getId());
        if (uploadConfig.isRequired() && hasNotUploadedFiles(obj.getItem())) {
            addError(errors, ERROR_VALIDATION_FILEREQUIRED, "/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        }
        return errors;
    }

    public boolean hasNotUploadedFiles(Item item) {
        try {
            return !itemService.hasUploadedFiles(item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
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

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
