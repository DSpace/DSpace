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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.ItemService;

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

    private static final String DOCUMENT_TYPE_FIELD = "dc.type";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValidation.class);

    private DCInputsReader inputReader;

    private ItemService itemService;

    private MetadataAuthorityService metadataAuthorityService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {

        String documentTypeValue = "";
        DCInputSet inputConfig = getInputReader().getInputsByFormName(config.getId());
        List<MetadataValue> documentType = itemService.getMetadataByMetadataString(obj.getItem(), DOCUMENT_TYPE_FIELD);
        if (documentType.size() > 0) {
            documentTypeValue = documentType.get(0).getValue();
        }

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
                            addError(ERROR_VALIDATION_REGEX,
                                "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                                input.getFieldName() + "/" + md.getPlace());
                        }
                        if (isAuthorityControlled) {
                            String authKey = md.getAuthority();
                            if (metadataAuthorityService.isAuthorityRequired(fieldKey) &&
                                StringUtils.isNotBlank(authKey)) {
                                addError(ERROR_VALIDATION_AUTHORITY_REQUIRED,
                                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() +
                                    "/" + input.getFieldName() + "/" + md.getPlace());
                            }
                        }
                    }
                    if ((input.isRequired() && mdv.size() == 0) && input.isVisible(DCInput.SUBMISSION_SCOPE)) {
                        // since this field is missing add to list of error
                        // fields
                        addError(ERROR_VALIDATION_REQUIRED,
                            "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + config.getId() + "/" +
                            input.getFieldName());
                    }
                }
            }
        }
        return getErrors();
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
