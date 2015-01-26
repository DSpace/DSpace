/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

/**
 * String values in use in journal landing page aspect.
 * @author Nathan Day
 */
public class Const {
    
    // parameters added to model during request validation action
    public static final String PARAM_JOURNAL_NAME = "journalName";
    public static final String PARAM_JOURNAL_ABBR = "journalAbbr";
    
    // names used in DRI
    public static final String BANNER_DIV = "journal-landing-banner";
    public static final String BANNER_PARA = "journal-landing-banner";
    
    // journal based search
    public static final String SEARCH_DIV = "journal-landing-search";
    
    // most recent deposits
    public static final String MOST_RECENT_DEPOSITS_DIV = "journal-landing-recent";
    public static final String MOST_RECENT_DEPOSITS_REFS = "journal-landing-recent-refs";
    
    public static final String TOPTEN_DOWNLOADS = "journal-landing-topten-downloads";
    public static final String TOPTEN_DOWNLOADS_MONTH = "journal-landing-topten-downloads-month";
    public static final String TOPTEN_DOWNLOADS_YEAR = "journal-landing-topten-downloads-year";
    public static final String TOPTEN_DOWNLOADS_ALLTIME = "journal-landing-topten-downloads-alltime";

    public static final String TOPTEN_VIEWS = "journal-landing-topten-views";
    public static final String TOPTEN_VIEWS_MONTH = "journal-landing-topten-views-month";
    public static final String TOPTEN_VIEWS_YEAR = "journal-landing-topten-views-year";
    public static final String TOPTEN_VIEWS_ALLTIME = "journal-landing-topten-views-alltime";
    
    public static final String USER_GEO = "journal-landing-user-geo";
    public static final String USER_GEO_VIEWS = "journal-landing-user-geo-views";
    public static final String USER_GEO_DOWNLOADS = "journal-landing-user-geo-downloads";
    
    public static final String ITEMS = "items";
    public static final String VALS = "vals";
    public static final String TABLIST = "tablist";
    public static final String EMPTY_VAL = "-";
    
    // TODO: make this this-month
    public static final String dateMonth = "dc.date.available_dt:[NOW-30DAY/DAY TO NOW]";
    public static final String dateYear = "dc.date.available_dt:[NOW-1YEAR/DAY TO NOW]";
    
}
