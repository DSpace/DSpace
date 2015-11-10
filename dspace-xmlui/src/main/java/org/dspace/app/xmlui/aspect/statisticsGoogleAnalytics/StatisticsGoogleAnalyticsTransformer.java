/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsGoogleAnalytics;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.google.GoogleQueryManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.google.api.services.analytics.model.GaData;

/**
 * User: Robin Taylor
 * Date: 11/07/2014
 * Time: 13:23
 */

public class StatisticsGoogleAnalyticsTransformer extends AbstractDSpaceTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statisticsGoogleAnalytics.title");
    private static final Message T_statisticsGoogleAnalytics_trail = message("xmlui.statisticsGoogleAnalytics.trail");

    private static final Message T_start_date = message("xmlui.statisticsGoogleAnalytics.dates.startDate");
    private static final Message T_end_date = message("xmlui.statisticsGoogleAnalytics.dates.endDate");
    private static final Message T_refresh = message("xmlui.statisticsGoogleAnalytics.dates.refresh");

    private static final Message T_page_views = message("xmlui.statisticsGoogleAnalytics.pageViews.title");
    private static final Message T_downloads = message("xmlui.statisticsGoogleAnalytics.downloads.title");

    private String startDateString = "";
    private String endDateString = "";
    private String handle;

    private static Logger log = Logger.getLogger(StatisticsGoogleAnalyticsTransformer.class);


    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        if(dso != null)
        {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
        }

        pageMeta.addTrailLink(contextPath + "/handle" + (dso != null && dso.getHandle() != null ? "/" + dso.getHandle() : "/google-stats"), T_statisticsGoogleAnalytics_trail);

        pageMeta.addMetadata("title").addContent(T_head_title);
    }


    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
        Calendar currentDate = Calendar.getInstance();
        String dateNow = formatter.format(currentDate.getTime());

        Calendar lastYearDate = Calendar.getInstance();
        lastYearDate.add(Calendar.YEAR, -1);
        String dateLastYear = formatter.format(lastYearDate.getTime());

        handle = HandleUtil.obtainHandle(objectModel).getHandle();

        Request request = ObjectModelHelper.getRequest(objectModel);

        if (request.getParameter("startDate") == null || request.getParameter("startDate").equals(""))
        {
            startDateString = dateLastYear;
        } else {
            startDateString = request.getParameter("startDate");
        }

        if (request.getParameter("endDate") == null || request.getParameter("endDate").equals(""))
        {
            endDateString = dateNow;
        } else {
            endDateString = request.getParameter("endDate");
        }

        Division main = body.addDivision("main");
        main.setHead(T_head_title);

        Division selectDate = main.addInteractiveDivision("selectDate", "", Division.METHOD_POST);
        List dates = selectDate.addList("dates", List.TYPE_FORM);

        Text startDate = dates.addItem().addText("startDate");
        startDate.setLabel(T_start_date);
        startDate.setValue(startDateString);

        Text endDate = dates.addItem().addText("endDate");
        endDate.setLabel(T_end_date);
        endDate.setValue(endDateString);

        dates.addItem().addButton("refresh").setValue(T_refresh);

        Division results = main.addDivision("results");
        getGAStuff(results);

    }

    private void getGAStuff(Division results) throws IOException, WingException {

        GoogleQueryManager gqm;
        GaData gaData;

        // Get the page views
        gqm = new GoogleQueryManager();
        gaData = gqm.getPageViews(startDateString, endDateString, handle);
        java.util.List<java.util.List<String>> rows = gaData.getRows();

        Table pageViews = results.addTable("pageViews", rows.size(), 2);
        pageViews.setHead(T_page_views);

        for (java.util.List<String> row : rows) {
            Row tableRow = pageViews.addRow();
            tableRow.addCellContent(row.get(0) + " / " + row.get(1));
            tableRow.addCellContent(row.get(2));
        }

        // Get the bitstream downloads
        gqm = new GoogleQueryManager();
        gaData = gqm.getBitstreamDownloads(startDateString, endDateString, handle);
        rows = gaData.getRows();

        Table bitstreamViews = results.addTable("downloads", rows.size(), 2);
        bitstreamViews.setHead(T_downloads);

        for (java.util.List<String> row : rows) {
            Row tableRow = bitstreamViews.addRow();
            tableRow.addCellContent(row.get(0) + " / " + row.get(1));
            tableRow.addCellContent(row.get(2));
        }
    }

}
