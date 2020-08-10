/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutTab;
import org.springframework.stereotype.Component;

/**
 * Implementation for CrisLayoutTab boxes patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/tabs/<:id> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "
 * /boxes/", "value": [{ ... box_object ... }]]'
 * </code>
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabBoxesAddOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_BOXES = "/boxes";

    private ObjectMapper objectMapper = new ObjectMapper();

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#perform
     * (org.dspace.core.Context, java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public D perform(Context context, D resource, Operation operation) throws SQLException {
        checkOperationValue(operation.getValue());
        if (supports(resource, operation)) {
            CrisLayoutTab tab = (CrisLayoutTab) resource;
            checkModelForExistingValue(tab);
            try {
                List<CrisLayoutBox> boxes = new ArrayList<>();
                JsonNode value = null;
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    value = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                } else {
                    value = objectMapper.readTree((String)operation.getValue());
                }
                if (value.isArray()) {
                    for (JsonNode v: value) {
                        CrisLayoutBox box = objectMapper.treeToValue(v, CrisLayoutBox.class);
                        boxes.add(box);
                    }
                } else {
                    CrisLayoutBox box = objectMapper.treeToValue(value, CrisLayoutBox.class);
                    boxes.add(box);
                }
                for (CrisLayoutBox box: boxes) {
                    tab.addBox(box);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException("CrisLayoutTabBoxesRemoveOperation does not support this operation");
        }
        return resource;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.patch.operation.PatchOperation#supports
     * (java.lang.Object, org.dspace.app.rest.model.patch.Operation)
     */
    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof CrisLayoutTab && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
                && operation.getPath().trim().startsWith(OPERATION_PATH_BOXES));
    }

    /**
     * Checks whether the boxes of Tab has an existing value to remove
     * @param CrisLayoutTab Object on which patch is being done
     */
    private void checkModelForExistingValue(CrisLayoutTab tab) {
        if (tab.getTab2Box() == null) {
            throw new DSpaceBadRequestException("Attempting to remove a non-existent value.");
        }
    }
}
