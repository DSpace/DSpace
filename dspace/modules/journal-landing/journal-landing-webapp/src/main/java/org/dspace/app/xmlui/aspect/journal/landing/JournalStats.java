/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
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
 * Add "Browse for data" div to journal landing page, containing a panel with
 * four tabs displaying recently published and most popular downloads.
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
    private static final Message T_empty            = message("xmlui.JournalLandingPage.JournalStats.empty");


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);

        divData = new DivData();
        TabData tb1 = new TabData();
        TabData tb2 = new TabData();
        TabData tb3 = new TabData();
        TabData tb4 = new TabData();
        tabData = Arrays.asList(tb1, tb2, tb3, tb4);

        divData.n = JOURNAL_STATS;
        divData.T_div_head = T_div_head;
        divData.maxResults = displayCount;

        tb1.n = JOURNAL_STATS_DEPS;
        tb1.buttonLabel = T_btnRecPub;
        tb1.dateFilter = solrDatePastMonth;
        tb1.refHead = T_mostRecent;
        tb1.valHead = T_date;
        tb1.queryType = QueryType.DEPOSITS;

        tb2.n = JOURNAL_STATS_MONTH;
        tb2.buttonLabel = T_btn_month;
        tb2.dateFilter = solrDatePastMonth;
        tb2.refHead = T_empty;
        tb2.valHead = T_ref_head;
        tb2.queryType = QueryType.DOWNLOADS;
        tb2.facetQueryField = facetQueryOwningId;

        tb3.n = JOURNAL_STATS_YEAR;
        tb3.buttonLabel = T_btn_year;
        tb3.dateFilter = solrDatePastYear;
        tb3.refHead = T_empty;
        tb3.valHead = T_ref_head;
        tb3.queryType = QueryType.DOWNLOADS;
        tb3.facetQueryField = facetQueryOwningId;

        tb4.n = JOURNAL_STATS_ALLTIME;
        tb4.buttonLabel = T_btn_alltime;
        tb4.dateFilter = solrDateAllTime;
        tb4.refHead = T_empty;
        tb4.valHead = T_ref_head;
        tb4.queryType = QueryType.DOWNLOADS;
        tb4.facetQueryField = facetQueryOwningId;
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        super.addStatsLists(body);
    }
}
