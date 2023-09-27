/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity Subscription to the REST data model
 *
 * @author Alba Aliu at atis.al
 *
 */
@Component
public class SubscriptionConverter implements DSpaceConverter<Subscription, SubscriptionRest> {

    @Autowired
    protected Utils utils;

    @Override
    public SubscriptionRest convert(Subscription subscription, Projection projection) {
        SubscriptionRest rest = new SubscriptionRest();
        rest.setProjection(projection);
        rest.setId(subscription.getID());
        List<SubscriptionParameterRest> subscriptionParameterRestList = new ArrayList<>();
        for (SubscriptionParameter subscriptionParameter : subscription.getSubscriptionParameterList()) {
            SubscriptionParameterRest subscriptionParameterRest = new SubscriptionParameterRest();
            subscriptionParameterRest.setName(subscriptionParameter.getName());
            subscriptionParameterRest.setValue(subscriptionParameter.getValue());
            subscriptionParameterRestList.add(subscriptionParameterRest);
        }
        rest.setSubscriptionParameterList(subscriptionParameterRestList);
        rest.setSubscriptionType(subscription.getSubscriptionType());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<Subscription> getModelClass() {
        return Subscription.class;
    }

}
