/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.google;

import com.google.api.services.analytics.model.GaData;

import java.io.IOException;


/**
 * User: Robin Taylor
 * Date: 20/08/2014
 * Time: 09:26
 */
public class GoogleQueryManager {

    public GaData getPageViews(String startDate, String endDate, String handle) throws IOException {
        return GoogleAccount.getInstance().getClient().data().ga().get(
                GoogleAccount.getInstance().getTableId(),
                startDate,
                endDate,
                "ga:pageviews") // Metrics.
                .setDimensions("ga:year,ga:month")
                .setSort("-ga:year,-ga:month")
                .setFilters("ga:pagePath=~/handle/" + handle + "$")
                .execute();
    }

    public GaData getBitstreamDownloads(String startDate, String endDate, String handle) throws IOException {
        return GoogleAccount.getInstance().getClient().data().ga().get(
                GoogleAccount.getInstance().getTableId(),
                startDate,
                endDate,
                "ga:totalEvents") // Metrics.
                .setDimensions("ga:year,ga:month")
                .setSort("-ga:year,-ga:month")
                .setFilters("ga:eventCategory==bitstream;ga:eventAction==download;ga:pagePath=~" + handle + "/")
                .execute();
    }

}

