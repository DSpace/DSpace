/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;

/**
 * Interface class which will be used to find all objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public interface DSpaceObjectUpdates {

    /**
     * Send an email to some addresses, concerning a Subscription, using a given dso.
     *
     * @param context current DSpace session.
     */
    @SuppressWarnings("rawtypes")
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency)
            throws SearchServiceException;

    default String findLastFrequency(String frequency) {
        String startDate = "";
        String endDate = "";
        Calendar cal = Calendar.getInstance();
        // Full ISO 8601 is e.g.
        SimpleDateFormat fullIsoStart = new SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'");
        SimpleDateFormat fullIsoEnd = new SimpleDateFormat("yyyy-MM-dd'T'23:59:59'Z'");
        switch (frequency) {
            case "D":
                cal.add(Calendar.DAY_OF_MONTH, -1);
                endDate = fullIsoEnd.format(cal.getTime());
                startDate = fullIsoStart.format(cal.getTime());
                break;
            case "M":
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                cal.add(Calendar.DAY_OF_MONTH, -dayOfMonth);
                endDate = fullIsoEnd.format(cal.getTime());
                cal.add(Calendar.MONTH, -1);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                startDate = fullIsoStart.format(cal.getTime());
                break;
            case "W":
                cal.add(Calendar.DAY_OF_WEEK, -1);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
                cal.add(Calendar.DAY_OF_WEEK, -dayOfWeek);
                endDate = fullIsoEnd.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, -6);
                startDate = fullIsoStart.format(cal.getTime());
                break;
            default:
                return null;
        }
        return "[" + startDate + " TO " + endDate + "]";
    }

}
