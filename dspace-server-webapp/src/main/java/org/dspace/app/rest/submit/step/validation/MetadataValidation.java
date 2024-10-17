/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import static org.dspace.app.rest.submit.step.DescribeStep.KEY_VALUE_SEPARATOR;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

/**
 * Execute three validation check on fields validation:
 * - mandatory metadata missing
 * - regex missing match
 * - authority required metadata missing
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class MetadataValidation extends AbstractValidation {

    private static final String ERROR_VALIDATION_REQUIRED = "error.validation.required";

    private static final String ERROR_VALIDATION_AUTHORITY_REQUIRED = "error.validation.authority.required";

    private static final String ERROR_VALIDATION_REGEX = "error.validation.regex";

    private static final String LOCAL_METADATA_HAS_CMDI = "local.hasCMDI";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValidation.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private MetadataAuthorityService metadataAuthorityService;

    private ConfigurationService configurationService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {

        List<ErrorRest> errors = new ArrayList<>();
        DCInputSet inputConfig = getInputReader().getInputsByFormName(config.getId());
        List<String> documentTypeValueList = new ArrayList<>();
        // Get the list of type-bind fields. It could be in the form of schema.element.qualifier=>metadata_field, or
        // just metadata_field
        List<String> typeBindFields = Arrays.asList(
                configurationService.getArrayProperty("submit.type-bind.field", new String[0]));

        for (String typeBindField : typeBindFields) {
            String typeBFKey = typeBindField;
            // If the type-bind field is in the form of schema.element.qualifier=>metadata_field, split it and get the
            // metadata field
            if (typeBindField.contains(KEY_VALUE_SEPARATOR)) {
                String[] parts = typeBindField.split(KEY_VALUE_SEPARATOR);
                // Get the second part of the split - the metadata field
                typeBFKey = parts[1];
            }
            // Get the metadata value for the type-bind field
            List<MetadataValue> documentType = itemService.getMetadataByMetadataString(obj.getItem(), typeBFKey);
            if (documentType.size() > 0) {
                documentTypeValueList.add(documentType.get(0).getValue());
            }
        }

        // Get list of all field names (including qualdrop names) allowed for this dc.type, or specific type-bind field
        List<String> allowedFieldNames = new ArrayList<>();

        if (CollectionUtils.isEmpty(documentTypeValueList)) {
            // If no dc.type is set, we allow all fields
            allowedFieldNames.addAll(inputConfig.populateAllowedFieldNames(null));
        } else {
            documentTypeValueList.forEach(documentTypeValue -> {
                allowedFieldNames.addAll(inputConfig.populateAllowedFieldNames(documentTypeValue));
            });
        }

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
                    //starting from the second element of the list and skipping one every time because the display
                    // values are also in the list and before the stored values.
                    for (int i = 1; i < inputPairs.size(); i += 2) {
                        String fullFieldname = input.getFieldName() + "." + (String) inputPairs.get(i);
                        List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fullFieldname);

                        // Check the lookup list. If no other inputs of the same field name allow this type,
                        // then remove. This includes field name without qualifier.
                        if (!input.isAllowedFor(documentTypeValueList) && (!allowedFieldNames.contains(fullFieldname)
                                && !allowedFieldNames.contains(input.getFieldName()))) {
                            itemService.removeMetadataValues(ContextUtil.obtainCurrentRequestContext(),
                                    obj.getItem(), mdv);
                        } else {
                            validateMetadataValues(mdv, input, config, isAuthorityControlled, fieldKey, errors);
                            if (mdv.size() > 0 && input.isVisible(DCInput.SUBMISSION_SCOPE)) {
                                foundResult = true;
                            }
                        }
                    }
                    if (input.isRequired() && !foundResult) {
                        // for this required qualdrop no value was found, add to the list of error fields
                        addError(errors, ERROR_VALIDATION_REQUIRED,
                                "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                        input.getFieldName());
                    }
                } else {
                    String fieldName = input.getFieldName();
                    if (fieldName != null) {
                        fieldsName.add(fieldName);
                    }
                }

                for (String fieldName : fieldsName) {
                    boolean valuesRemoved = false;
                    List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fieldName);
                    if (!input.isAllowedFor(documentTypeValueList)) {
                        // Check the lookup list. If no other inputs of the same field name allow this type,
                        // then remove. Otherwise, do not
                        if (!(allowedFieldNames.contains(fieldName))) {
                            itemService.removeMetadataValues(ContextUtil.obtainCurrentRequestContext(),
                                    obj.getItem(), mdv);
                            valuesRemoved = true;
                            log.debug("Stripping metadata values for " + input.getFieldName() + " on type "
                                    + documentTypeValueList + " as it is allowed by another input of the same field " +
                                    "name");
                        } else {
                            log.debug("Not removing unallowed metadata values for " + input.getFieldName() + " on type "
                                    + documentTypeValueList + " as it is allowed by another input of the same field " +
                                    "name");
                        }
                    }
                    validateMetadataValues(mdv, input, config, isAuthorityControlled, fieldKey, errors);
                    if (((input.isRequired() && mdv.size() == 0) && input.isVisible(DCInput.SUBMISSION_SCOPE)
                            && !valuesRemoved)
                            || !isValidComplexDefinitionMetadata(input, mdv)) {
                        // Is the input required for *this* type? In other words, are we looking at a required
                        // input that is also allowed for this document type
                        if (input.isAllowedFor(documentTypeValueList)) {
                            // since this field is missing add to list of error
                            // fields
                            addError(errors, ERROR_VALIDATION_REQUIRED, "/"
                                    + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                    input.getFieldName());
                        }
                    }
                    if (LOCAL_METADATA_HAS_CMDI.equals(fieldName)) {
                        try {
                            Context context = ContextUtil.obtainCurrentRequestContext();
                            CMDIFileBundleMaintainer.updateCMDIFileBundle(context, obj.getItem(), mdv);
                        } catch (AuthorizeException | IOException exception) {
                            log.error("Cannot update CMDI file bundle (ORIGINAL/METADATA) because: " +
                                    exception.getMessage());
                        }
                    }
                }
            }

        }

        return errors;
    }

    /**
     * Check if the metadata values for a complex definition input are valid.
     * Valid if:
     * - the complex input field is required and all required nested input fields are filled in.
     * - the complex input field is not required, if there is a valued in the nested input field - all required nested
     *      input fields must be filled in.
     * - the complex input field is not required, and none of the nested input fields are required.
     */
    private boolean isValidComplexDefinitionMetadata(DCInput input, List<MetadataValue> mdv) {
        // The input is not a complex definition - do not validate it
        if (!input.getInputType().equals("complex")) {
            return true;
        }

        // Get the complex definition nested inputs
        Map<String, Map<String, String>> complexDefinitionInputs = input.getComplexDefinition().getInputs();

        // Check valid state of the complex definition input when it is required
        if (input.isRequired()) {
            // There are no values in the complex input field
            if (CollectionUtils.isEmpty(mdv)) {
                return false;
            }
        } else {
            // The complex input field is not required
            if (CollectionUtils.isEmpty(mdv)) {
                // There are no values in the complex input field
                return true;
            }
        }
        return checkAllRequiredInputFieldsAreFilledIn(complexDefinitionInputs, mdv);
    }

    /**
     * Check if all required nested input fields are filled in.
     */
    private boolean checkAllRequiredInputFieldsAreFilledIn(Map<String, Map<String, String>> complexDefinitionInputs,
                                                        List<MetadataValue> mdv) {
        // If any of the nested input fields are filled in - all required nested input fields must be filled in
        int complexDefinitionIndex = -1;
        // Go through all nested input fields
        for (String complexDefinitionInputName : complexDefinitionInputs.keySet()) {
            complexDefinitionIndex++;

            // Get the definition of the nested input field
            Map<String, String> complexDefinitionInputValues =
                    complexDefinitionInputs.get(complexDefinitionInputName);
            // Check if the nested input field is required - if not do not check if it is filled in
            if (!StringUtils.equals(BooleanUtils.toStringTrueFalse(true),
                    complexDefinitionInputValues.get("required"))) {
                continue;
            }

            // Load filled in values of the nested input field
            List<String> filledInputValues = new ArrayList<>(Arrays.asList(
                    mdv.get(0).getValue().split(DCInput.ComplexDefinitions.getSeparator(),-1)));

            // Check if the required nested input field is filled in. It is valid if there is a value in the nested
            // input.
            if (!StringUtils.isBlank(filledInputValues.get(complexDefinitionIndex))) {
                continue;
            }

            // EU identifier must have `openaire_id` value otherwise the `openaire_id` could be empty.
            if (StringUtils.equals("openaire_id", complexDefinitionInputName) &&
                    !StringUtils.equals("euFunds", filledInputValues.get(0))) {
                continue;
            }
            return false;

        }
        return true;
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

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setMetadataAuthorityService(MetadataAuthorityService metadataAuthorityService) {
        this.metadataAuthorityService = metadataAuthorityService;
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

    public void setInputReader(DCInputsReader inputReader) {
        this.inputReader = inputReader;
    }

}
