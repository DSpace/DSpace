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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.util.TypeBindUtils;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.validation.model.ValidationError;
import org.dspace.workflow.WorkflowItem;

/**
 * Execute three validation check on fields validation: - mandatory metadata
 * missing - regex missing match - authority required metadata missing
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4sciente.it)
 */
public class MetadataValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_REQUIRED = "error.validation.required";

    private static final String ERROR_VALIDATION_AUTHORITY_REQUIRED = "error.validation.authority.required";

    private static final String ERROR_VALIDATION_REGEX = "error.validation.regex";

    private static final String ERROR_VALIDATION_NOT_REPEATABLE = "error.validation.notRepeatable";

    private static final Logger log = LogManager.getLogger(MetadataValidator.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private ConfigurationService configurationService;

    private MetadataAuthorityService metadataAuthorityService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {

        List<ValidationError> errors = new ArrayList<>();

        DCInputSet inputConfig = getDCInputSet(config);
        String documentType = TypeBindUtils.getTypeBindValue(obj);

        // Get list of all field names (including qualdrop names) allowed for this dc.type
        List<String> allowedFieldNames = inputConfig.populateAllowedFieldNames(documentType);

        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                String fieldKey = metadataAuthorityService.makeFieldKey(input.getSchema(), input.getElement(),
                        input.getQualifier());
                boolean isAuthorityControlled = metadataAuthorityService.isAuthorityAllowed(fieldKey, Constants.ITEM,
                        obj.getCollection());

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
                        if (!input.isAllowedFor(documentType) &&  (!allowedFieldNames.contains(fullFieldname)
                                && !allowedFieldNames.contains(input.getFieldName()))) {
                            removeMetadataValues(context, obj.getItem(), mdv);
                        } else {
                            validateMetadataValues(obj.getCollection(), mdv, input, config, isAuthorityControlled,
                                fieldKey, errors);
                            if (mdv.size() > 0 && (input.isVisible(DCInput.SUBMISSION_SCOPE) ||
                                    input.isVisible(DCInput.WORKFLOW_SCOPE))) {
                                foundResult = true;
                            }
                        }
                    }
                    if (input.isRequired() && !foundResult) {
                        // for this required qualdrop no value was found, add to the list of error fields
                        addError(errors, ERROR_VALIDATION_REQUIRED,
                            "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
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
                    if (!input.isAllowedFor(documentType)) {
                        // Check the lookup list. If no other inputs of the same field name allow this type,
                        // then remove. Otherwise, do not
                        if (!(allowedFieldNames.contains(fieldName))) {
                            removeMetadataValues(context, obj.getItem(), mdv);
                            valuesRemoved = true;
                            log.debug("Stripping metadata values for " + input.getFieldName() + " on type "
                                    + documentType + " as it is allowed by another input of the same field " +
                                    "name");
                        } else {
                            log.debug("Not removing unallowed metadata values for " + input.getFieldName() + " on type "
                                    + documentType + " as it is allowed by another input of the same field " +
                                    "name");
                        }
                    }
                    validateMetadataValues(obj.getCollection(), mdv, input, config,
                        isAuthorityControlled, fieldKey, errors);
                    if ((input.isRequired() && mdv.size() == 0)
                            && (input.isVisible(DCInput.SUBMISSION_SCOPE)
                            || (obj instanceof WorkflowItem && input.isVisible(DCInput.WORKFLOW_SCOPE)))
                            && !valuesRemoved) {
                        // Is the input required for *this* type? In other words, are we looking at a required
                        // input that is also allowed for this document type
                        if (input.isAllowedFor(documentType)) {
                            // since this field is missing add to list of error
                            // fields
                            addError(errors, ERROR_VALIDATION_REQUIRED,
                                "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                    input.getFieldName());
                        }
                    }
                }
            }
        }
        return errors;
    }

    private DCInputSet getDCInputSet(SubmissionStepConfig config) {
        try {
            return getInputReader().getInputsByFormName(config.getId());
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateMetadataValues(Collection collection, List<MetadataValue> metadataValues, DCInput input,
        SubmissionStepConfig config, boolean isAuthorityControlled, String fieldKey, List<ValidationError> errors) {
        // if the filed is not repeatable, it should have only one value
        if (!input.isRepeatable() && metadataValues.size() > 1) {
            for (int i = 0; i < metadataValues.size(); i++) {
                addError(errors, ERROR_VALIDATION_NOT_REPEATABLE,
                        "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + input.getFieldName() + "/" + i);
            }
        }

        for (MetadataValue md : metadataValues) {
            if (! (input.validate(md.getValue()))) {
                addError(errors, ERROR_VALIDATION_REGEX,
                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                        input.getFieldName() + "/" + md.getPlace());
            }
            if (isAuthorityControlled) {
                String authKey = md.getAuthority();
                if (metadataAuthorityService.isAuthorityRequired(md.getMetadataField(), Constants.ITEM, collection) &&
                    StringUtils.isBlank(authKey)) {
                    addError(errors, ERROR_VALIDATION_AUTHORITY_REQUIRED,
                        "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() +
                            "/" + input.getFieldName() + "/" + md.getPlace());
                }
            }
        }
    }

    private void removeMetadataValues(Context context, Item item, List<MetadataValue> metadataValues) {
        try {
            itemService.removeMetadataValues(context, item, metadataValues);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setMetadataAuthorityService(MetadataAuthorityService metadataAuthorityService) {
        this.metadataAuthorityService = metadataAuthorityService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
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

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
