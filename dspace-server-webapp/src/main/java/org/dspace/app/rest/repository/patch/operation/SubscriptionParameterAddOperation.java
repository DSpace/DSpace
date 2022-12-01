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
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



/**
 * Implementation for SubscriptionParameterAddOperation patches.
 * <p>
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}api/core/subscriptions/<:id-subscription> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * </code>
 */
@Component
public class SubscriptionParameterAddOperation extends PatchOperation<Subscription> {

    @Autowired
    private SubscribeService subscribeService;


    @Override
    public Subscription perform(Context context, Subscription subscription, Operation operation)
            throws SQLException {
        if (supports(subscription, operation)) {
            JsonNode value = null;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    value = ((JsonValueEvaluator) operation.getValue()).getValueNode();
                } else {
                    value = objectMapper.readTree((String) operation.getValue());
                }
                SubscriptionParameterRest subscriptionParameterRest =
                        objectMapper.readValue(value.toString(), SubscriptionParameterRest.class);
                SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
                subscriptionParameter.setSubscription(subscription);
                subscriptionParameter.setValue(subscriptionParameterRest.getValue());
                subscriptionParameter.setName(subscriptionParameterRest.getName());
                subscribeService.addSubscriptionParameter(context, subscription.getID(), subscriptionParameter);
            } catch (UnprocessableEntityException e) {
                throw new UnprocessableEntityException(e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            throw new DSpaceBadRequestException("Subscription does not support this operation");
        }
        return subscription;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Subscription
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD));
    }

}
