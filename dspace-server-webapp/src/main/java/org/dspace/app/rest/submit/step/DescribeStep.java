/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.model.step.DataDescribe;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.util.ObjectUtils;

/**
 * Describe step for DSpace Spring Rest. Expose and allow patching of the in progress submission metadata. It is
 * configured via the config/submission-forms.xml file
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class DescribeStep extends AbstractProcessingStep {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DescribeStep.class);

    // Input reader for form configuration
    private DCInputsReader inputReader;
    // Configuration service
    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    public DescribeStep() throws DCInputsReaderException {
        inputReader = new DCInputsReader();
    }

    @Override
    public DataDescribe getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config) {
        DataDescribe data = new DataDescribe();
        try {
            DCInputSet inputConfig = inputReader.getInputsByFormName(config.getId());
            readField(obj, config, data, inputConfig);
        } catch (DCInputsReaderException e) {
            log.error(e.getMessage(), e);
        }
        return data;
    }

    private void readField(InProgressSubmission obj, SubmissionStepConfig config, DataDescribe data,
                           DCInputSet inputConfig) throws DCInputsReaderException {
        String documentTypeValue = "";
        List<MetadataValue> documentType = itemService.getMetadataByMetadataString(obj.getItem(),
                configurationService.getProperty("submit.type-bind.field", "dc.type"));
        if (documentType.size() > 0) {
            documentTypeValue = documentType.get(0).getValue();
        }

        // Get list of all field names (including qualdrop names) allowed for this dc.type
        List<String> allowedFieldNames = inputConfig.populateAllowedFieldNames(documentTypeValue);

        // Loop input rows and process submitted metadata
        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                List<String> fieldsName = new ArrayList<String>();
                if (input.isQualdropValue()) {
                    for (Object qualifier : input.getPairs()) {
                        fieldsName.add(input.getFieldName() + "." + (String) qualifier);
                    }
                } else {
                    fieldsName.add(input.getFieldName());
                }


                for (String fieldName : fieldsName) {
                    List<MetadataValue> mdv = itemService.getMetadataByMetadataString(obj.getItem(),
                                                                                      fieldName);
                    for (MetadataValue md : mdv) {
                        MetadataValueRest dto = new MetadataValueRest();
                        dto.setAuthority(md.getAuthority());
                        dto.setConfidence(md.getConfidence());
                        dto.setLanguage(md.getLanguage());
                        dto.setPlace(md.getPlace());
                        dto.setValue(md.getValue());

                        String[] metadataToCheck = Utils.tokenize(md.getMetadataField().toString());
                        if (data.getMetadata().containsKey(
                            Utils.standardize(metadataToCheck[0], metadataToCheck[1], metadataToCheck[2], "."))) {
                            // If field is allowed by type bind, add value to existing field set, otherwise remove
                            // all values for this field
                            if (allowedFieldNames.contains(fieldName)) {
                                data.getMetadata()
                                        .get(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                md.getMetadataField().getElement(),
                                                md.getMetadataField().getQualifier(),
                                                "."))
                                        .add(dto);
                            } else {
                                data.getMetadata().remove(Utils.standardize(metadataToCheck[0], metadataToCheck[1],
                                        metadataToCheck[2], "."));
                            }
                        } else {
                            // Add values only if allowed by type bind
                            if (allowedFieldNames.contains(fieldName)) {
                                List<MetadataValueRest> listDto = new ArrayList<>();
                                listDto.add(dto);
                                data.getMetadata()
                                        .put(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                md.getMetadataField().getElement(),
                                                md.getMetadataField().getQualifier(),
                                                "."), listDto);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {

        String[] pathParts = op.getPath().substring(1).split("/");
        DCInputSet inputConfig = inputReader.getInputsByFormName(stepConf.getId());
        if ("remove".equals(op.getOp()) && pathParts.length < 3) {
            // manage delete all step fields
            String[] path = op.getPath().substring(1).split("/", 3);
            String configId = path[1];
            List<String> fieldsName = getInputFieldsName(inputConfig, configId);
            for (String fieldName : fieldsName) {
                String fieldPath = op.getPath() + "/" + fieldName;
                Operation fieldRemoveOp = new RemoveOperation(fieldPath);
                PatchOperation<MetadataValueRest> patchOperation = new PatchOperationFactory()
                     .instanceOf(DESCRIBE_STEP_METADATA_OPERATION_ENTRY, fieldRemoveOp.getOp());
                patchOperation.perform(context, currentRequest, source, fieldRemoveOp);
            }
        } else {
            PatchOperation<MetadataValueRest> patchOperation = new PatchOperationFactory()
                        .instanceOf(DESCRIBE_STEP_METADATA_OPERATION_ENTRY, op.getOp());
            String[] split = patchOperation.getAbsolutePath(op.getPath()).split("/");
            if (inputConfig.isFieldPresent(split[0])) {
                patchOperation.perform(context, currentRequest, source, op);
                String mappedToIfNotDefault = this.getMappedToIfNotDefault(split[0], inputConfig);
                if (StringUtils.isBlank(mappedToIfNotDefault)) {
                    return;
                }

                // if the complex input field contains `mapped-to-if-not-default` definition
                // put additional data to the defined metadata from the `mapped-to-if-not-default`
                Operation newOp = this.getOperationWithChangedMetadataField(op, mappedToIfNotDefault, source);
                if (ObjectUtils.isEmpty(newOp)) {
                    return;
                }

                patchOperation = new PatchOperationFactory()
                        .instanceOf(DESCRIBE_STEP_METADATA_OPERATION_ENTRY, newOp.getOp());
                patchOperation.perform(context, currentRequest, source, newOp);
            } else {
                throw new UnprocessableEntityException("The field " + split[0] + " is not present in section "
                                                                                   + inputConfig.getFormName());
            }
        }
    }

    private List<String> getInputFieldsName(DCInputSet inputConfig, String configId) throws DCInputsReaderException {
        List<String> fieldsName = new ArrayList<String>();
        for (DCInput[] row : inputConfig.getFields()) {
            for (DCInput input : row) {
                if (input.isQualdropValue()) {
                    for (Object qualifier : input.getPairs()) {
                        fieldsName.add(input.getFieldName() + "." + (String) qualifier);
                    }
                } else if (StringUtils.equalsIgnoreCase(input.getInputType(), "group") ||
                        StringUtils.equalsIgnoreCase(input.getInputType(), "inline-group")) {
                    log.info("Called child form:" + configId + "-" +
                        Utils.standardize(input.getSchema(), input.getElement(), input.getQualifier(), "-"));
                    DCInputSet inputConfigChild = inputReader.getInputsByFormName(configId + "-" + Utils
                        .standardize(input.getSchema(), input.getElement(), input.getQualifier(), "-"));
                    fieldsName.addAll(getInputFieldsName(inputConfigChild, configId));
                } else {
                    fieldsName.add(input.getFieldName());
                }
            }
        }
        return fieldsName;
    }

    /**
     *
     * @param oldOp old operation from the FE
     * @param mappedToIfNotDefault metadata where will be stored data from FE request
     * @return a new operation which is created from the old one but the metadata is changed
     */
    private Operation getOperationWithChangedMetadataField(Operation oldOp, String mappedToIfNotDefault,
                                                           InProgressSubmission source) {
        String[] oldOpPathArray = oldOp.getPath().split("/");
        String[] opPathArray = oldOpPathArray.clone();

        // metadata field could has more metadata values
        // TO DO problem je v tom, ze sa volala replace metoda pri add
        // potrebujem zvlast odchytit ADD metodu a REPLACE
        boolean isNotFirstValue = false;
        boolean removeMetadata = false;

        // change the metadata (e.g. `local.sponsor`) in the end of the path
        if (NumberUtils.isCreatable(opPathArray[opPathArray.length - 1])) {
            // e.g. `traditional/section/local.sponsor/0`
            opPathArray[opPathArray.length - 2] = mappedToIfNotDefault;
            isNotFirstValue = true;
        } else {
            // e.g. `traditional/section/local.sponsor`
            opPathArray[opPathArray.length - 1] = mappedToIfNotDefault;
        }

        // from the old operation load the value of the input field
        String oldOpValue = "";
        JsonNode jsonNodeValue = null;

        // Operation has a value wrapped in the JsonValueEvaluator
        JsonValueEvaluator jsonValEvaluator = (JsonValueEvaluator) oldOp.getValue();
        Iterator<JsonNode> jsonNodes = jsonValEvaluator.getValueNode().elements();

        if (isNotFirstValue) {
            // replace operation has value wrapped in the ObjectNode
            jsonNodeValue = jsonValEvaluator.getValueNode().get("value");
        } else {
            // add operation has value wrapped in the ArrayNode
            for (Iterator<JsonNode> it = jsonNodes; it.hasNext(); ) {
                JsonNode jsonNode = it.next();
                if (jsonNode instanceof ObjectNode) {
                    jsonNodeValue = jsonNode.get("value");
                }
            }
        }

        if (ObjectUtils.isEmpty(jsonNodeValue) || StringUtils.isBlank(jsonNodeValue.asText())) {
            throw new UnprocessableEntityException("Cannot load JsonNode value from the operation: " +
                    oldOp.getPath());
        }
        // get the value from the old operation as a string
        oldOpValue = jsonNodeValue.asText();

        // add the value from the old operation to the new operation
        String opValue = "";
        if (StringUtils.equals("local.sponsor", oldOpPathArray[oldOpPathArray.length - 1]) ||
            StringUtils.equals("local.sponsor", oldOpPathArray[oldOpPathArray.length - 2])) {
            // for the metadata `local.sponsor` change the `info:eu-repo...` value from the old value

            // load info:eu-repo* from the jsonNodeValue
            // the eu info is on the 4th index of the complexInputType
            List<String> complexInputValue = Arrays.asList(oldOpValue.split(";"));
            if (complexInputValue.size() > 4) {
                String euIdentifier = complexInputValue.get(4);
                // remove last value from the eu identifier - it should be in the metadata value
                List<String> euIdentifierSplit = new ArrayList<>(Arrays.asList(euIdentifier.split("/")));
                if (euIdentifierSplit.size() == 6) {
                    euIdentifierSplit.remove(5);
                }

                euIdentifier = String.join("/", euIdentifierSplit);
                opValue = euIdentifier;
            } else if (oldOp instanceof ReplaceOperation) {
                // replace from Non EU sponsor to EU sponsor - remove EU identifier
                removeMetadata = true;
            } else {
                // non EU sponsor doesn't have dc.relation
                return null;
            }
        }

        // the opValue wasn't updated
        if (StringUtils.isBlank(opValue)) {
            // just copy old value to the new operation
            opValue = oldOpValue;
        }

        // create a new operation and add the new value there
        JsonNodeFactory js = new JsonNodeFactory(false);
        // ArrayNode - Add operation
        ArrayNode an = new ArrayNode(js);
        an.add(js.textNode(opValue));

        // ObjectNode - Replace operation
        ObjectNode on = new ObjectNode(js);
        on.set("value", js.textNode(opValue));

        Operation newOp = null;
        // create a path as a string for the new operation
        String opPath = String.join("/", opPathArray);

        // load the actual value from the metadataField which will be updated
        List<MetadataValue> metadataByMetadataString = itemService.getMetadataByMetadataString(source.getItem(),
                mappedToIfNotDefault);

        if (oldOp instanceof AddOperation) {
            if (isNotFirstValue) {
                // index of the value in the metadataField. Metadata field could have more values.
//                int index = Integer.parseInt(opPathArray[opPathArray.length - 1]);

                // trying to replace the value of the metadataField in the index which is bigger than
                // maximum size of values in the updating metadataFields
                // e.g. there is only one value in the `dc.relation` metadataField and we need to update
                // `dc.relation` in the index `1` which is second value from the `dc.relation`
                if (metadataByMetadataString.isEmpty()) {
                    // update value in the metadataField but the metadataField is empty - call Add operation
                    // from the path remove index because Add operation doesn't have the index
                    opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length - 1);
                    opPath = String.join("/", opPathArray);
                    return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
                } else {
                    opPathArray[opPathArray.length - 1] = String.valueOf(metadataByMetadataString.size());
                    opPath =  String.join("/", opPathArray);
                    return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
                }
            }
            return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
        } else {
            // remove
            if (removeMetadata) {
                opPathArray[opPathArray.length - 1] = String.valueOf(metadataByMetadataString.size() - 1);
                opPath =  String.join("/", opPathArray);
                return new RemoveOperation(opPath);
            }

            // cannot replace value to the metadataField which not exist in the Item - the Add operation must be called
            // add new
            if (metadataByMetadataString.isEmpty()) {
                opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length - 1);
                opPath = String.join("/", opPathArray);
                return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
            }

            // index of the value in the metadataField. Metadata field could have more values.
            int index = Integer.parseInt(opPathArray[opPathArray.length - 1]);

            if (index == metadataByMetadataString.size()) {
                // update value in the metadataField but the metadataField is empty - call Add operation
                if (index == 0) {
                    // from the path remove index because Add operation doesn't have the index
                    opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length - 1);
                    opPath = String.join("/", opPathArray);
                    return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
                } else {
                    opPathArray[opPathArray.length - 1] = String.valueOf(metadataByMetadataString.size());
                    opPath =  String.join("/", opPathArray);
                    return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
                }
            }

            // trying to replace the value of the metadataField in the index which is bigger than
            // maximum size of values in the updating metadataFields
            // e.g. there is only one value in the `dc.relation` metadataField and we need to update
            // `dc.relation` in the index `1` which is second value from the `dc.relation`
            if (index > metadataByMetadataString.size()) {
                // from the path decrease the metadataField value index to actual size of the metadataField values
                opPathArray[opPathArray.length - 1] = String.valueOf(metadataByMetadataString.size() - 1);
                opPath =  String.join("/", opPathArray);
                return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
            }
            return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
        }
//        if (isNotFirstValue) {
//            // cannot replace value to the metadataField which not exist in the Item - the Add operation must
//            be called
//            if (metadataByMetadataString.isEmpty()) {
//                return null;
//            }
//
//            // index of the value in the metadataField. Metadata field could have more values.
//            int index = Integer.parseInt(opPathArray[opPathArray.length - 1]);
//
//            // trying to replace the value of the metadataField in the index which is bigger than
//            // maximum size of values in the updating metadataFields
//            // e.g. there is only one value in the `dc.relation` metadataField and we need to update
//            // `dc.relation` in the index `1` which is second value from the `dc.relation`
//            if (index >= metadataByMetadataString.size()) {
//                // update value in the metadataField but the metadataField is empty - call Add operation
//                if (index == 0) {
//                    // from the path remove index because Add operation doesn't have the index
//                    opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length - 1);
//                    opPath = String.join("/", opPathArray);
//                    return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
//                } else {
//                    // from the path decrease the metadataField value index to actual size of the metadataField values
//                    opPathArray[opPathArray.length - 1] = String.valueOf(metadataByMetadataString.size() - 1);
//                    opPath =  String.join("/", opPathArray);
//                    return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
//                }
//            }
//            return new ReplaceOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
//        } else {
////            if (metadataByMetadataString.isEmpty()) {
////                // new metadata is not there
////                opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length -1);
////                String opPathAddOp = String.join("/", opPathArray);
////                newOp = new AddOperation(opPathAddOp, new JsonValueEvaluator(new ObjectMapper(), an));
////            } else if (Integer.parseInt(opPathArray[opPathArray.length-1]) > metadataByMetadataString.size()) {
////                // metadata is not there but is another input field
////                if (Integer.parseInt(opPathArray[opPathArray.length-1])-1 == 0) {
////                    // new from 1 to 0
////                    opPathArray = ArrayUtils.remove(opPathArray, opPathArray.length -1);
////                    String opPathAddOp = String.join("/", opPathArray);
////                    newOp = new AddOperation(opPathAddOp, new JsonValueEvaluator(new ObjectMapper(), an));
////                } else {
////                    // from 2 to 1
////                    opPathArray[opPathArray.length-1] = String.valueOf(Integer.parseInt
////                      (opPathArray[opPathArray.length-1]) - 1);
////                    opPath =  String.join("/", opPathArray);
////                    newOp = new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
////                }
////            } else {
////                // new
////                newOp = new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), on));
////            }
//            // first new
//            return new AddOperation(opPath, new JsonValueEvaluator(new ObjectMapper(), an));
//        }
    }

    /**
     * Load the `mapped-to-if-not-default` from the complex input field definition
     * @param complexDefinition
     * @return NULL - the `mapped-to-if-not-default` is not defined OR value - the mapped-to-if-not-default is defined
     */
    private String loadMappedToIfNotDefaultFromComplex(DCInput.ComplexDefinition complexDefinition) {
        Map<String, Map<String, String>> inputs = complexDefinition.getInputs();
        for (String inputName : inputs.keySet()) {
            Map<String, String> inputDefinition = inputs.get(inputName);
            for (String inputDefinitionValue : inputDefinition.keySet()) {
                if (StringUtils.equals(inputDefinitionValue, "mapped-to-if-not-default")) {
                    return inputDefinition.get(inputDefinitionValue);
                }
            }
        }
        return null;
    }

    /**
     * From the input configuration load the `mapped-to-if-not-default` definition in the complex input field
     * definitions
     * @param inputFieldMetadata the metadata where should be stored data from the FE request
     * @param inputConfig current input fields configuratino
     * @return NULL - the `mapped-to-if-not-default` is not defined OR value - the mapped-to-if-not-default is defined
     */
    private String getMappedToIfNotDefault(String inputFieldMetadata, DCInputSet inputConfig) {
        List<DCInput[]> inputsListOfList = Arrays.asList(inputConfig.getFields());
        for (DCInput[] inputsList : inputsListOfList) {
            List<DCInput> inputs = Arrays.asList(inputsList);
            for (DCInput input : inputs) {
                if (!StringUtils.equals("complex", input.getInputType())) {
                    break;
                }

                String[] metadataFieldName = inputFieldMetadata.split("\\.");
                if (!StringUtils.equals(metadataFieldName[0], input.getSchema()) ||
                        !StringUtils.equals(metadataFieldName[1], input.getElement()) ||
                        (metadataFieldName.length > 2 &&
                                !StringUtils.equals(metadataFieldName[2], input.getQualifier()))) {
                    break;
                }

                String mappedToIfNotDefault = this.loadMappedToIfNotDefaultFromComplex(input.getComplexDefinition());
                if (StringUtils.isNotBlank(mappedToIfNotDefault)) {
                    return mappedToIfNotDefault;
                }
            }
        }
        return null;
    }
}
