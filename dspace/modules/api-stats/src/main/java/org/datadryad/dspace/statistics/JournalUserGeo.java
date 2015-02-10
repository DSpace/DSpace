/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datadryad.dspace.statistics;

import org.apache.log4j.Logger;
import static org.datadryad.dspace.statistics.SolrUtils.*;
import org.dspace.core.Context;

/**
 *
 * @author Nathan Day
 */
public class JournalUserGeo {

    private static final Logger LOGGER = Logger.getLogger(JournalUserGeo.class);
    private Context context = null;

    private static final String downloadByCountry = "/select/?q=type:0&facet=true&facet.field=countryCode";
    private static final String viewByCountry = "/select/?q=type:2&facet=true&facet.field=countryCode";
    private static final String allTime = "time:[ * TO NOW ]";
    
    private String responseDownload;
    private String responseView;
    
    public JournalUserGeo(Context context) {
        this.context = context;
        getStats();
    }
    // TODO: update queries to return response with filtering by item id as well
    private void getStats() {
        responseDownload = getSolrResponseCount(SolrUtils.solrStatsUrlBase, downloadByCountry, allTime);
        responseView     = getSolrResponseCount(SolrUtils.solrStatsUrlBase, viewByCountry, allTime);
    }
    public String getResponseDownload() { return this.responseDownload; }
    public String getResponseView()     { return this.responseView;     }
}
