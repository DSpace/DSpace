/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.eperson.service.SubscriptionParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for SubscriptionParameterRemoveOperation patches.
 * <p>
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}api/core/subscriptions/<:id-subscription> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * </code>
 */
@Component
public class SubscriptionParameterRemoveOperation extends PatchOperation<Subscription> {

    @Autowired
    private SubscriptionParameterService subscriptionParameterService;
    @Autowired
    private SubscribeService subscribeService;


    @Override
    public Subscription perform(Context context, Subscription subscription, Operation operation)
            throws SQLException {
        if (supports(subscription, operation)) {
            Integer path = Integer.parseInt(operation.getPath().split("/")[2]);
            try {
                SubscriptionParameter subscriptionParameter = subscriptionParameterService.findById(context, path);
                subscribeService.removeSubscriptionParameter(context, subscription.getID(), subscriptionParameter);
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException("Unauthorized user for removing subscription parameter");
            }
        } else {
            throw new DSpaceBadRequestException("Subscription does not support this operation");

        }
        return subscription;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof Subscription
                && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE));
    }

}
