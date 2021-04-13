/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.service.CrisLayoutMetric2BoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



/**
 * Implementation for CrisLayoutBoxMetricsConfiguration patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/layout/boxmetricsconfiguration/<:box-id> --H
 * "Authorization: Bearer ..." -H 'Content-Type: application/json' --data
 * '[{"op":"add","path":"/metrics/", "value":["metric1", "metric2"]]'
 * </code>
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxMetricsConfigurationAddOperation<D> extends PatchOperation<D> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_CONFIGURATION_PATH = "^/metrics/[-]*$";

    private static final String OPERATION_CONFIGURATION_ADD_PATH = "^/metrics/$";

    private static final String OPERATION_CONFIGURATION_APPEND_PATH = "^/metrics/-$";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CrisLayoutMetric2BoxService metric2BoxService;

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
                if (value.isArray()) {
                    if (operation.getPath().matches(OPERATION_CONFIGURATION_ADD_PATH)) {
                        this.metric2BoxService.addMetrics(context, box, getMetrics(value));
                    }
                    if (operation.getPath().matches(OPERATION_CONFIGURATION_APPEND_PATH)) {
                        this.metric2BoxService.appendMetrics(context, box, getMetrics(value));
                    }
                }
            } catch (UnprocessableEntityException e) {
                throw new UnprocessableEntityException(e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException
            ("CrisLayoutBoxMetricsConfigurationAddOperation does not support this operation");
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

    private List<String> getMetrics(JsonNode value) {
        List<String> metrics = Lists.newArrayList();
        for (JsonNode v: value) {
            metrics.add(v.asText());
        }
        return metrics;
    }

}
