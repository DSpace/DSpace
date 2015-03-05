/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;


import org.xml.sax.SAXException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;
import org.dspace.app.xmlui.aspect.journal.landing.JournalLandingTabbedTransformer.TabData;
import org.dspace.app.xmlui.utils.UIException;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.*;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
/**
 *
 * @author Nathan Day
 */
public class JournalStats extends JournalLandingTabbedTransformer {

    private static final Logger log = Logger.getLogger(JournalStats.class);

    private static final Message T_div_head         = message("xmlui.JournalLandingPage.JournalStats.panel_head");
    private static final Message T_mostRecent       = message("xmlui.JournalLandingPage.JournalStats.empty");
    private static final Message T_date             = message("xmlui.JournalLandingPage.JournalStats.date");
    private static final Message T_btnRecPub        = message("xmlui.JournalLandingPage.JournalStats.rec_pub");
    private static final Message T_btn_month        = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.month");
    private static final Message T_btn_year         = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.year");
    private static final Message T_btn_alltime      = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.alltime");
    private static final Message T_ref_head         = message("xmlui.JournalLandingPage.JournalStats.val_head");
    private static final Message T_ref_head_month   = message("xmlui.JournalLandingPage.JournalStats.ref_head_month");
    private static final Message T_ref_head_year    = message("xmlui.JournalLandingPage.JournalStats.ref_head_year");
    private static final Message T_ref_head_alltime = message("xmlui.JournalLandingPage.JournalStats.ref_head_alltime");


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        int currentMonth = getCurrentMonth();
        int currentYear = getCurrentYear();
        String currentMonthStr = getCurrentMonthStr();

        divData = new DivData();
        tabData = new ArrayList<TabData>(3);
        TabData tb1 = new TabData();
        TabData tb2 = new TabData();
        TabData tb3 = new TabData();
        TabData tb4 = new TabData();
        tabData.add(tb1);
        tabData.add(tb2);
        tabData.add(tb3);
        tabData.add(tb4);

        divData.n = JOURNAL_STATS;
        divData.T_div_head = T_div_head;
        divData.facetQueryField = facetQueryOwningId;
        divData.maxResults = displayCount;

        tb1.n = JOURNAL_STATS_DEPS;
        tb1.buttonLabel = T_btnRecPub;
        tb1.dateFilter = String.format(solrDateFormat, getCurrentYear(), getCurrentMonth());
        tb1.refHead = T_mostRecent;
        tb1.valHead = T_date;
        tb1.queryType = QueryType.DEPOSITS;

        tb2.n = JOURNAL_STATS_MONTH;
        tb2.buttonLabel = T_btn_month;
        tb2.dateFilter = String.format(solrDateFormat, currentYear, currentMonth);
        tb2.refHead = T_ref_head_month.parameterize(currentMonthStr,currentYear);
        tb2.valHead = T_ref_head;
        tb2.queryType = QueryType.DOWNLOADS;

        tb3.n = JOURNAL_STATS_YEAR;
        tb3.buttonLabel = T_btn_year;
        tb3.dateFilter = String.format(solrDateFormat, currentYear, januaryInd);
        tb3.refHead = T_ref_head_year.parameterize(currentYear);
        tb3.valHead = T_ref_head;
        tb3.queryType = QueryType.DOWNLOADS;

        tb4.n = JOURNAL_STATS_ALLTIME;
        tb4.buttonLabel = T_btn_alltime;
        tb4.dateFilter = solrDateAllTime;
        tb4.refHead = T_ref_head_alltime;
        tb4.valHead = T_ref_head;
        tb4.queryType = QueryType.DOWNLOADS;
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        super.addStatsTable(body);
    }
}
