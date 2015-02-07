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
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;

/**
 *
 * @author Nathan Day
 */
public class TopTenDownloads extends JournalLandingTabbedTransformer {
    
    private static final Logger log = Logger.getLogger(TopTenDownloads.class);

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        int currentMonth = getCurrentMonth();
        int currentYear = getCurrentYear();
        String currentMonthStr = getCurrentMonthStr();
        itemType = Constants.BITSTREAM;

        divData = new DivData();
        divData.n = TOPTEN_DOWNLOADS;
        divData.T_div_head = message("xmlui.JournalLandingPage.TopTenDownloads.panel_head");

        tabData = new ArrayList<TabData>(3);
        TabData tb1 = new TabData();
        tb1.n = TOPTEN_DOWNLOADS_MONTH;
        tb1.buttonLabel = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.month");
        tb1.dateFilter = String.format(solrDateFormat, currentYear, currentMonth);
        tb1.refHead = message("xmlui.JournalLandingPage.TopTenDownloads.ref_head_month").parameterize(currentMonthStr,currentYear);
        tb1.valHead = message("xmlui.JournalLandingPage.TopTenDownloads.val_head");
        tabData.add(tb1);

        TabData tb2 = new TabData();
        tb2.n = TOPTEN_DOWNLOADS_YEAR;
        tb2.buttonLabel = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.year");
        tb2.dateFilter = String.format(solrDateFormat, currentYear - 1, currentMonth);
        tb2.refHead = message("xmlui.JournalLandingPage.TopTenDownloads.ref_head_year").parameterize(currentYear);
        tb2.valHead = message("xmlui.JournalLandingPage.TopTenDownloads.val_head");
        tabData.add(tb2);

        TabData tb3 = new TabData();
        tb3.n = TOPTEN_DOWNLOADS_ALLTIME;
        tb3.buttonLabel = message("xmlui.JournalLandingPage.JournalLandingTabbedTransformer.alltime");
        tb3.dateFilter = solrDateAllTime;
        tb3.refHead = message("xmlui.JournalLandingPage.TopTenDownloads.ref_head_alltime");
        tb3.valHead = message("xmlui.JournalLandingPage.TopTenDownloads.val_head");
        tabData.add(tb3);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        super.addBody(body);
    }
}
