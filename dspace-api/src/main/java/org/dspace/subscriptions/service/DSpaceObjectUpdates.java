/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.service;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Interface class which will be used to find
 * all objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public interface DSpaceObjectUpdates {

    /**
     * Send an email to some addresses, concerning a Subscription, using a given
     * dso.
     *
     * @param context current DSpace session.
     */
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency) throws SearchServiceException;

    default LocalDate findLastFrequency(String frequency) {
        LocalDate now = LocalDate.now(); // now
        Calendar cal = Calendar.getInstance();
        switch (frequency) {
            case "daily":
                return now.minusDays(1);
            case "monthly":
                return now.minusMonths(1);
            case "weekly":
                return now.minusWeeks(1);
            default:
                return null;
        }
    }

}
