/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.solr.client.solrj.SolrQuery;

/**
 * String values in use in journal landing page aspect.
 * @author Nathan Day
 */
public class Const {

    // parameters added to model during request validation action
    public static final String PARAM_JOURNAL_NAME = "journalName";
    public static final String PARAM_JOURNAL_ABBR = "journalAbbr";

    // banner
    public static final String BANNER_DIV_OUTER = "journal-landing-banner-outer";
    public static final String BANNER_DIV_INNER = "journal-landing-banner-inner";
    public static final String BANNER_MEM = "journal-landing-banner-mem";
    public static final String BANNER_PAY = "journal-landing-banner-pay";
    public static final String BANNER_DAT = "journal-landing-banner-dat";
    public static final String BANNER_AUT = "journal-landing-banner-aut";
    public static final String BANNER_MET = "journal-landing-banner-met";
    public static final String BANNER_INT = "journal-landing-banner-int";
    public static final String BANNER_PAC = "journal-landing-banner-pac";

    public static final String SEARCH_DIV = "journal-landing-search";

    // most recent deposits
    public static final String JOURNAL_STATS = "journal-landing-stats";
    public static final String JOURNAL_STATS_DEPS = "journal-landing-stats-deps";
    public static final String JOURNAL_STATS_MONTH = "journal-landing-stats-month";
    public static final String JOURNAL_STATS_YEAR = "journal-landing-stats-year";
    public static final String JOURNAL_STATS_ALLTIME = "journal-landing-stats-alltime";

    public static final String ITEMS = "items";
    public static final String VALS = "vals";
    public static final String TABLIST = "tablist";

    public static enum QueryType { DOWNLOADS, DEPOSITS };

    // date format string for most recent deposits
    public static final String januaryInd = "01";
    public static final String fmtDateView = "yyyy-MM-dd";
    public static final String solrDateFormat = "time:[%d-%s-01T00:00:00.000Z TO NOW]";
    public static final String solrDateAllTime = "[* TO NOW]";
    public static final int displayCount = 10;
    public static final String depositsDisplayField = "dc.date.accessioned_dt";
    public static final String depositsDisplaySortField = "dc.date.accessioned_dt";
    public static final String dcDateAccessioned = "dc.date.accessioned";
    public static final SolrQuery.ORDER depositsDisplaySortOrder = SolrQuery.ORDER.desc;
    public static final String facetQueryId = "id";
    public static final String facetQueryOwningId = "owningItem";
    public static final String facetQueryCountryCode = "countryCode";
}