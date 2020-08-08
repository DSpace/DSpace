/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.MetadataField;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for CrisLayoutBoxMetadataConfiguration patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/boxmetadataconfiguration/<:box-id> --H
 * "Authorization: Bearer ..." -H 'Content-Type: application/json' --data
 * '[{"op":"add","path":"/rows/0/<:fields>/0", "value":{"metadata":"orgunit.identifier.name",
 * "label":"Department Name", "rendering":"browselink", "fieldType":"metadata", "style":null }}]'
 * </code>
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxConfigurationAddOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_CONFIGURATION_PATH = "^/rows/[0-9]+/fields/.*$";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MetadataFieldService metadataService;

    @Autowired
    private CrisLayoutFieldService fieldService;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#perform
     * (org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            CrisLayoutBox box = (CrisLayoutBox) resource;
            try {
                JsonNode value = null;
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    value = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                } else {
                    value = objectMapper.readTree((String)operation.getValue());
                }
                Integer row = null;
                Integer prosition = null;
                String[] tks = operation.getPath().split("/");
                if (tks != null && tks.length > 0) {
                    for (int i = 0; i < tks.length; i++) {
                        if (tks[i].equalsIgnoreCase("rows") && tks.length > i + 1) {
                            row = parseInteger(tks[++i]);
                        } else if (tks[i].equalsIgnoreCase("fields") && tks.length > i + 1) {
                            prosition = parseInteger(tks[++i]);
                        }
                    }
                }
                if (value.isArray()) {
                    for (JsonNode v: value) {
                        CrisLayoutField layoutField = getLayoutFieldFromJsonNode(context, row, v);
                        layoutField = fieldService.create(context, layoutField);
                        box.addLayoutField(layoutField, prosition);
                    }
                } else {
                    CrisLayoutField layoutField = getLayoutFieldFromJsonNode(context, row, value);
                    layoutField = fieldService.create(context, layoutField);
                    box.addLayoutField(layoutField, prosition);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException
            ("CrisLayoutBoxConfigurationAddOperation does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#supports
     * (java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutBox && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && operation.getPath().matches(OPERATION_CONFIGURATION_PATH));
    }

    /**
     * Create new CrisLayoutField from json node
     * @param context DSpace current context
     * @param row
     * @param priority
     * @param node json value
     * @return
     * @throws JsonProcessingException
     * @throws SQLException
     */
    private CrisLayoutField getLayoutFieldFromJsonNode (Context context, Integer row, JsonNode node)
            throws JsonProcessingException, SQLException {
        CrisLayoutField field = new CrisLayoutField();
        field.setRow(row);

        JsonNode metadataNode = node.get("metadata");
        if (metadataNode != null && metadataNode.asText() != null ) {
            String metadata = metadataNode.asText();
            MetadataField metadataField = metadataService.findByString(context, metadata, '.');
            field.setMetadataField(metadataField);
        }

        JsonNode labelNode = node.get("label");
        if (labelNode != null && labelNode.asText() != null ) {
            field.setLabel(labelNode.asText());
        }

        JsonNode renderingNode = node.get("rendering");
        if (renderingNode != null && renderingNode.asText() != null ) {
            field.setRendering(renderingNode.asText());
        }

        JsonNode fieldTypeNode = node.get("fieldType");
        if (fieldTypeNode != null && fieldTypeNode.asText() != null ) {
            field.setType(fieldTypeNode.asText());
        }

        JsonNode styleNode = node.get("style");
        if (styleNode != null && styleNode.asText() != null ) {
            field.setStyle(styleNode.asText());
        }

        JsonNode bundleNode = node.get("bundle");
        if (bundleNode != null && bundleNode.asText() != null ) {
            field.setStyle(bundleNode.asText());
        }
        return field;
    }

    /**
     * Returns an Integer object holding the value of the specified String,
     * if the string cannot be parsed as an integer returns null
     * @param val
     * @return
     */
    private Integer parseInteger(String val) {
        Integer value = null;
        try {
            value = Integer.valueOf(val);
        } catch ( Exception e ) {
            value = null;
        }
        return value;
    }
}
