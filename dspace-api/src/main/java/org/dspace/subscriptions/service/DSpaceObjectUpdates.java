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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;


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

    default String findLastFrequency(String frequency) {
        GregorianCalendar localCalendar = new GregorianCalendar(2021, 3, 5);
        localCalendar.setTime(new Date());
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        // Full ISO 8601 is e.g. "2009-07-16T13:59:21Z"
        SimpleDateFormat fullIsoStart = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'");
        SimpleDateFormat fullIsoEnd = new SimpleDateFormat("yyyy-MM-dd'T'23:59:59'Z'");
        localCalendar.setTimeZone(utcZone);
        // Now set the UTC equivalent.
        switch (frequency) {
            case "daily":
                localCalendar.add(GregorianCalendar.DAY_OF_YEAR, -1);
                break;
            case "monthly":
                localCalendar.add(GregorianCalendar.MONTH, -1);
                break;
            case "weekly":
                localCalendar.add(GregorianCalendar.WEEK_OF_MONTH, -1);
                break;
            default:
                return null;
        }
        return "[" + fullIsoStart.format(localCalendar.getTime()) + " TO " + fullIsoEnd.format(localCalendar.getTime()) + "]";
    }

}
