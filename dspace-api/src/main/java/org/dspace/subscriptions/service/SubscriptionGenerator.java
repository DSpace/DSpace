/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.service;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Interface Class which will be used to send email notifications to ePerson
 * containing information for all list of objects.
 *
 * @author Alba Aliu
 */
public interface SubscriptionGenerator<T> {

    public void notifyForSubscriptions(Context c, EPerson ePerson, List<T> comm, List<T> coll);

}