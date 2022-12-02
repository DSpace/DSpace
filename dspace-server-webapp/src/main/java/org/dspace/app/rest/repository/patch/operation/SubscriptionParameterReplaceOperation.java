/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscriptionParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for SubscriptionParameterReplaceOperation patches.
 * <p>
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}api/core/subscriptions/<:id-subscription> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * </code>
 */
@Component
public class SubscriptionParameterReplaceOperation extends PatchOperation<Subscription> {

    @Autowired
    private SubscriptionParameterService subscriptionParameterService;

    @Override
    public Subscription perform(Context context, Subscription subscription, Operation operation)
            throws SQLException {
        if (supports(subscription, operation)) {
            Integer subscriptionParameterId = Integer.parseInt(operation.getPath().split("/", 3)[2]);
            checkModelForExistingValue(subscription, subscriptionParameterId);
            JsonNode value = null;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    value = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                } else {
                    value = objectMapper.readTree((String) operation.getValue());
                }
                SubscriptionParameterRest subscriptionParameterRest = objectMapper.readValue(
                        value.toString(), SubscriptionParameterRest.class);
                subscriptionParameterService.edit(context, subscriptionParameterId,subscriptionParameterRest.getValue(),
                                                  subscriptionParameterRest.getName(), subscription);
            } catch (UnprocessableEntityException e) {
                throw new UnprocessableEntityException(e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return subscription;
        } else {
            throw new DSpaceBadRequestException("Subscription does not support this operation");
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Subscription && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE));
    }

    /**
     * Checks whether the subscription
     */
    @SuppressWarnings("ReturnValueIgnored")
    private void checkModelForExistingValue(Subscription subscription, Integer id) {
        subscription.getSubscriptionParameterList().stream().filter(subscriptionParameter -> {
            return subscriptionParameter.getId().equals(id);
        }).findFirst().orElseThrow();
    }

}