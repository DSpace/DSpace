/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import java.util.Set;

import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * Service interface class for the subscription e-mail notification services
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface SubscriptionEmailNotificationService {

    /**
     * Performs sending of e-mails to subscribers by frequency value and SubscriptionType
     * 
     * @param context           DSpace context object
     * @param handler           Applicable DSpaceRunnableHandler
     * @param subscriptionType  Currently supported only "content"
     * @param frequency         Valid values include: D (Day), W (Week) and M (Month)
     */
    public void perform(Context context, DSpaceRunnableHandler handler, String subscriptionType, String frequency);

    /**
     *  returns a set of supported SubscriptionTypes
     */
    public Set<String> getSupportedSubscriptionTypes();

}
