/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import static org.dspace.app.rest.submit.step.validation.MetadataValidation.ERROR_VALIDATION_AUTHORITY_REQUIRED;
import static org.dspace.app.rest.submit.step.validation.MetadataValidation.ERROR_VALIDATION_REGEX;
import static org.dspace.app.rest.submit.step.validation.MetadataValidation.ERROR_VALIDATION_REQUIRED;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.BitstreamService;
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

    private DCInputsReader inputReader;

    private ItemService itemService;

    private UploadConfigurationService uploadConfigurationService;

    private MetadataAuthorityService metadataAuthorityService;

    private BitstreamService bitstreamService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {
        List<ErrorRest> errors = new ArrayList<>();
        UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(config.getId());
        if (uploadConfig.isRequired() && !itemService.hasUploadedFiles(obj.getItem())) {
            addError(errors, ERROR_VALIDATION_FILEREQUIRED,
                     "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/"
                         + config.getId());
        }
        if (itemService.hasUploadedFiles(obj.getItem())) {
            List<Bitstream> bitstreams =
                    itemService.getNonInternalBitstreams(ContextUtil.obtainCurrentRequestContext(), obj.getItem());
            for (Bitstream bitstream : bitstreams) {
                errors.addAll(metadataValidate(bitstream, config, uploadConfig));
            }
        }
        return errors;
    }

    private List<ErrorRest> metadataValidate(Bitstream obj, SubmissionStepConfig config,
                                             UploadConfiguration uploadConfig)
            throws DCInputsReaderException {
        List<ErrorRest> errors = new ArrayList<>();
        String documentTypeValue = "";
        DCInputSet inputConfig;
        inputConfig = getInputReader().getInputsByFormName(uploadConfig.getMetadata());


        // Begin the actual validation loop
        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                String fieldKey =
                        metadataAuthorityService.makeFieldKey(input.getSchema(), input.getElement(),
                                input.getQualifier());
                boolean isAuthorityControlled = metadataAuthorityService.isAuthorityControlled(fieldKey);

                List<String> fieldsName = new ArrayList<String>();

                if (input.isQualdropValue()) {
                    boolean foundResult = false;
                    List<Object> inputPairs = input.getPairs();
                    for (int i = 1; i < inputPairs.size(); i += 2) {
                        String fullFieldname = input.getFieldName() + "." + (String) inputPairs.get(i);
                        List<MetadataValue> mdv = bitstreamService.getMetadataByMetadataString(obj, fullFieldname);

                        validateMetadataValues(mdv, input, config, isAuthorityControlled, fieldKey, errors);
                        if (!mdv.isEmpty() && input.isVisible(DCInput.SUBMISSION_SCOPE)) {
                            foundResult = true;
                        }
                    }
                    if (input.isRequired() && !foundResult) {
                        // for this required qualdrop no value was found, add to the list of error fields
                        addError(errors, ERROR_VALIDATION_REQUIRED,
                                "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                        input.getFieldName());
                    }
                } else {
                    String name = input.getFieldName();
                    if (name != null) {
                        fieldsName.add(name);
                    }
                }

                for (String fieldName : fieldsName) {
                    boolean valuesRemoved = false;
                    List<MetadataValue> mdv = bitstreamService.getMetadataByMetadataString(obj, fieldName);
                    validateMetadataValues(mdv, input, config, isAuthorityControlled, fieldKey, errors);
                    if ((input.isRequired() && mdv.isEmpty()) && input.isVisible(DCInput.SUBMISSION_SCOPE)
                            && !valuesRemoved) {
                        // Is the input required for *this* type? In other words, are we looking at a required
                        // input that is also allowed for this document type
                        if (input.isAllowedFor(documentTypeValue)) {
                            // since this field is missing add to list of error
                            // fields
                            addError(errors, ERROR_VALIDATION_REQUIRED, "/"
                                    + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                    input.getFieldName());
                        }
                    }
                }
            }
        }
        return errors;
    }

    private void validateMetadataValues(List<MetadataValue> mdv, DCInput input, SubmissionStepConfig config,
                                        boolean isAuthorityControlled, String fieldKey,
                                        List<ErrorRest> errors) {
        for (MetadataValue md : mdv) {
            if (! (input.validate(md.getValue()))) {
                addError(errors, ERROR_VALIDATION_REGEX,
                        "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                input.getFieldName() + "/" + md.getPlace());
            }
            if (isAuthorityControlled) {
                String authKey = md.getAuthority();
                if (metadataAuthorityService.isAuthorityRequired(fieldKey) &&
                        StringUtils.isBlank(authKey)) {
                    addError(errors, ERROR_VALIDATION_AUTHORITY_REQUIRED,
                            "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() +
                                    "/" + input.getFieldName() + "/" + md.getPlace());
                }
            }
        }
    }

    public DCInputsReader getInputReader() {
        if (inputReader == null) {
            try {
                inputReader = new DCInputsReader();
            } catch (DCInputsReaderException e) {
                log.error(e.getMessage(), e);
            }
        }
        return inputReader;
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

    public void setInputReader(DCInputsReader inputReader) {
        this.inputReader = inputReader;
    }

    public MetadataAuthorityService getMetadataAuthorityService() {
        return metadataAuthorityService;
    }

    public void setMetadataAuthorityService(MetadataAuthorityService metadataAuthorityService) {
        this.metadataAuthorityService = metadataAuthorityService;
    }

    public BitstreamService getBitstreamService() {
        return bitstreamService;
    }

    public void setBitstreamService(BitstreamService bitstreamService) {
        this.bitstreamService = bitstreamService;
    }


}
