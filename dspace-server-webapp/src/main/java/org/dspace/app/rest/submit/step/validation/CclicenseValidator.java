/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.license.CreativeCommonsServiceImpl;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.dspace.app.rest.repository.WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS;


/**
 * This class validates that the Creative Commons License has been granted for the
 * in-progress submission.
 *
 * @author Mattia Vianelli (Mattia.Vianelli@4science.com)
 */
public class CclicenseValidator extends AbstractValidation {

    /**
     * Construct a Creative Commons License configuration.
     * @param configurationService DSpace configuration provided by the DI container.
     */
    @Inject
    public CclicenseValidator(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private final ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CreativeCommonsServiceImpl creativeCommonsService;

    public static final String ERROR_VALIDATION_CCLICENSEREQUIRED = "error.validation.cclicense.required";

    private String name;

    private Boolean required;

    /**
     * Validate the license of the item.
     * @param item The item whose cclicense is to be validated.
     * @param config The configuration for the submission step for cclicense.
     * @return A list of validation errors.
     */
    private List<ErrorRest> validateLicense(Item item, SubmissionStepConfig config) {
        List<ErrorRest> errors = new ArrayList<>();

        String licenseURI = creativeCommonsService.getLicenseURI(item);
        if (licenseURI == null || licenseURI.isBlank()) {
            addError(errors, ERROR_VALIDATION_CCLICENSEREQUIRED, "/" + OPERATION_PATH_SECTIONS + "/" + config.getId());
        }

        return errors;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Check if at least one Creative Commons License is required when submitting a new Item.
     * @return true if a Creative Commons License is required setting true for the property cc.license.required.
     */
    public Boolean isRequired() {
        if (required == null) {
            return configurationService.getBooleanProperty("cc.license.required", false);
        }
        return required;
    }

    /**
     * Set whether at least one Creative Commons License is required when submitting a new Item.
     * @param required true if a Creative Commons License is required.
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Perform validation on the item and config(ccLicense).
     * @param obj The submission to be validated.
     * @param config The configuration for the submission step for cclicense.
     * @return A list of validation errors.
     * @throws SQLException If there is a problem accessing the database.
     */

    @Override
    public List<? extends ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj, SubmissionStepConfig config) throws DCInputsReaderException, SQLException {
        List<ErrorRest> errors = new ArrayList<>();


        if (this.isRequired() && obj != null && obj.getItem() != null) {
            errors.addAll(validateLicense(obj.getItem(), config));
        }

        return errors;
    }

    public void setName(String name) {
        this.name = name;
    }

}
