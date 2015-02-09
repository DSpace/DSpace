/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.datadryad.dspace.statistics;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import static org.datadryad.dspace.statistics.SolrUtils.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 *
 * @author Nathan Day
 */
public class JournalUserGeo {

    private static final Logger log = Logger.getLogger(JournalUserGeo.class);
    private Context context = null;

    private ArrayList<String> viewCountries = new ArrayList<String>();
    private ArrayList<String> viewCounts = new ArrayList<String>();

    public JournalUserGeo(Context context) {
        this.context = context;
    }

    private void getStats() {
        String a = getSolrResponseCount(SolrUtils.solrSearchUrlBase, "location:l2 AND DSpaceStatus:Archived", "dc.date.issued_dt:[NOW-30DAY TO NOW]");
    }

}
