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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;

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

    private static final String DOCUMENT_TYPE_FIELD = "dc.type";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValidator.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private MetadataAuthorityService metadataAuthorityService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {

        List<ValidationError> errors = new ArrayList<>();

        DCInputSet inputConfig = getDCInputSet(config);
        List<MetadataValue> documentType = itemService.getMetadataByMetadataString(obj.getItem(), DOCUMENT_TYPE_FIELD);
        String documentTypeValue = documentType.size() > 0 ? documentType.get(0).getValue() : "";

        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                String fieldKey =
                    metadataAuthorityService.makeFieldKey(input.getSchema(), input.getElement(), input.getQualifier());
                boolean isAuthorityControlled = metadataAuthorityService.isAuthorityControlled(fieldKey);

                // skip validation if field is not allowed for the current document type
                if (input.isAllowedFor(documentTypeValue) == false) {
                    continue;
                }

                List<String> fieldsName = new ArrayList<String>();
                if (input.isQualdropValue()) {
                    for (Object qualifier : input.getPairs()) {
                        fieldsName.add(input.getFieldName() + "." + (String) qualifier);
                    }
                } else {
                    fieldsName.add(input.getFieldName());
                }

                for (String fieldName : fieldsName) {
                    List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(), fieldName);
                    for (MetadataValue md : mdv) {
                        if (!(input.validate(md.getValue()))) {
                            addError(errors, ERROR_VALIDATION_REGEX,
                                "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                input.getFieldName() + "/" + md.getPlace());
                        }
                        if (isAuthorityControlled) {
                            String authKey = md.getAuthority();
                            if (metadataAuthorityService.isAuthorityRequired(fieldKey) &&
                                StringUtils.isNotBlank(authKey)) {
                                addError(errors, ERROR_VALIDATION_AUTHORITY_REQUIRED,
                                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() +
                                    "/" + input.getFieldName() + "/" + md.getPlace());
                            }
                        }
                    }
                    if ((input.isRequired() && mdv.size() == 0) && input.isVisible(DCInput.SUBMISSION_SCOPE)) {
                        // since this field is missing add to list of error
                        // fields
                        addError(errors, ERROR_VALIDATION_REQUIRED,
                            "/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" + input.getFieldName());
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

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
