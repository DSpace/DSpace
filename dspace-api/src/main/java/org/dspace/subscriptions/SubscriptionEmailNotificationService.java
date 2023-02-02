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
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface SubscriptionEmailNotificationService {

    public void perform(Context context, DSpaceRunnableHandler handler, String type, String frequency);

    public Set<String> getSupportedSubscriptionTypes();

}
