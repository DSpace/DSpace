/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.dspace.app.xmlui.wing.Message;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

/**
 * @author Nathan Day
 */
public class Const {

    // parameters added to model during request validation action
    public static final String PARAM_JOURNAL_ISSN = "journalISSN";
    public static final String PARAM_JOURNAL_NAME = "journalName";
    public static final String PARAM_JOURNAL_ABBR = "journalAbbr";

    // banner
    public static final String BANNER_DIV_OUTER = "journal-landing-banner-outer";
    public static final String BANNER_DIV_INNER = "journal-landing-banner-inner";
    public static final String BANNER_MEM = "journal-landing-banner-mem";
    public static final String BANNER_SPO = "journal-landing-banner-spo";
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

    public enum QueryType { DOWNLOADS, DEPOSITS, SOLR_RESULTS };

    public static final String fmtDateView = "yyyy-MM-dd";
    public static final String dcDateAccessioned = "dc.date.accessioned";
    public static final String CINCLUDE_META = "cinclude";
    public static final String CINCLUDE_SOLR_BASE = "solr-base-url";
    public static final String SPACE = " ";
    public static final String URL_SPACE = "%20";

    // DryadJournal
    public static final String archivedDataFilesQuery    = "SELECT * FROM ArchivedPackageDataFileItemIdsByJournal(?)";
    public static final String archivedDataFilesQueryCol =               "archivedpackagedatafileitemidsbyjournal";

    public static final String archivedDataPackageIds    = "SELECT * FROM ArchivedPackageItemIdsByJournal(?,?);";
    public static final String archivedDataPackageIdsCol =               "archivedpackageitemidsbyjournal";

    public static final String archivedPackageCount      = "SELECT * FROM ArchivedPackageCountByJournal(?)";
    public static final String archivedPackageCountCol   =               "archivedpackagecountbyjournal";

    public static final Message T_div_head         = message("xmlui.JournalLandingPage.JournalStats.panel_head");
    public static final Message T_mostRecent       = message("xmlui.JournalLandingPage.JournalStats.empty");
    public static final Message T_date             = message("xmlui.JournalLandingPage.JournalStats.date");
    public static final Message T_btnRecPub        = message("xmlui.JournalLandingPage.JournalStats.rec_pub");
    public static final Message T_btn_month        = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.month");
    public static final Message T_btn_year         = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.year");
    public static final Message T_btn_alltime      = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.alltime");
    public static final Message T_ref_head         = message("xmlui.JournalLandingPage.JournalStats.val_head");
    public static final Message T_empty            = message("xmlui.JournalLandingPage.JournalStats.empty");
}