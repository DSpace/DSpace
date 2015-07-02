/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.statistics.StatisticsLoader;
import org.dspace.app.xmlui.aspect.artifactbrowser.StatisticsViewer;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Transformer to display statistics data in XML UI.
 */
public class MonthlyStatistics extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(StatisticsViewer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_choose_report = message("xmlui.ArtifactBrowser.StatisticsViewer.choose_month");

    private static final Message T_page_title = message("xmlui.ArtifactBrowser.StatisticsViewer.report.title");

    private static final Message T_empty_title = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.title");

    private static final Message T_empty_text = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.text");

    private static final SimpleDateFormat sdfDisplay = new SimpleDateFormat(
            "MM'/'yyyy");

    private static final SimpleDateFormat sdfLink = new SimpleDateFormat(
            "yyyy'-'M");

    private boolean initialised = false;

    private String reportDate = null;

    private SourceValidity validity;

    /**
     * DRUM Customizations for Monthly Statistics
     */
    private static final String strDspace = ConfigurationManager
            .getProperty("dspace.dir");

    private static final File dir = new File(strDspace + "/stats/monthly");

    /**
     * Get the caching key for this report.
     */
    @Override
    public Serializable getKey()
    {
        initialise();

        if (reportDate != null)
        {
            return reportDate;
        }

        return "general";
    }

    /**
     * Generate the validity for this cached entry.
     */
    @Override
    public SourceValidity getValidity()
    {
        if (validity == null)
        {
            try
            {
                initialise();
                boolean showReport = ConfigurationManager
                        .getBooleanProperty("report.public");

                // If the report isn't public
                if (!showReport)
                {
                    try
                    {
                        // Administrators can always view reports
                        showReport = AuthorizeManager.isAdmin(context);
                    }
                    catch (SQLException sqle)
                    {
                        log.error("Unable to check for administrator", sqle);
                    }
                }

                // Only generate a validity if the report is visible
                if (showReport)
                {
                    File analysisFile = null;

                    // Get a file for the report data
                    if (reportDate != null)
                    {
                        analysisFile = StatisticsLoader
                                .getAnalysisFor(reportDate);
                    }
                    else
                    {
                        analysisFile = StatisticsLoader.getGeneralAnalysis();
                    }

                    if (analysisFile != null)
                    {
                        // Generate the validity based on the properties of the
                        // report data file
                        DSpaceValidity newValidity = new DSpaceValidity();
                        newValidity.add(Long.toString(analysisFile
                                .lastModified()));
                        newValidity.add("-");
                        newValidity.add(Long.toString(analysisFile.length()));
                        validity = newValidity.complete();
                    }
                }
            }
            catch (Exception e)
            {

            }
        }

        return validity;
    }

    /**
     * Add additional navigation options. This is to allow selection of a
     * monthly report
     *
     * @param options
     * @throws SAXException
     * @throws WingException
     * @throws UIException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    // @Override
    // public void addOptions(Options options) throws SAXException,
    // WingException,
    // UIException, SQLException, IOException, AuthorizeException
    // {
    // Date[] monthlyDates = StatisticsLoader.getMonthlyAnalysisDates();
    //
    // if (monthlyDates != null && monthlyDates.length > 0)
    // {
    // List statList = options.addList("statsreports");
    // statList.setHead(T_choose_report);
    //
    // HashMap<String, String> params = new HashMap<String, String>();
    // // for (Date date : monthlyDates)
    // // {
    // // params.put("date", sdfLink.format(date));
    // // statList.addItemXref(super.generateURL("statistics", params),
    // // sdfDisplay.format(date));
    // // }
    // }
    // }

    /**
     * Add title, etc. metadata
     *
     * @param pageMeta
     * @throws SAXException
     * @throws WingException
     * @throws UIException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        initialise();

        pageMeta.addMetadata("title").addContent(T_page_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_page_title);
    }

    /**
     * Output the body of the report
     *
     * @param body
     * @throws SAXException
     * @throws WingException
     * @throws UIException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        initialise();
        boolean publicise = ConfigurationManager
                .getBooleanProperty("report.public");

        // Check that the reports are either public, or user is an administrator
        if (!publicise && !AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException();
        }

        // Create the renderer for the results
        Division div = body.addDivision("statistics", "primary");

        Request request = ObjectModelHelper.getRequest(objectModel);

        List monthlyReports = div.addList("Monthly Reports");
        // List the available stat files
        File aFiles[] = dir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                return (name.endsWith("_stats.txt") && !name
                        .endsWith("current_stats.txt"));
            }
        });

        if (aFiles.length == 0)
        {
            div.setHead(T_empty_title);
            div.addPara(T_empty_text);
        }

        java.util.Arrays.sort(aFiles, new Comparator()
        {
            @Override
            public int compare(Object o1, Object o2)
            {
                String s1 = ((File) o1).getName();
                String s2 = ((File) o2).getName();
                return s1.compareTo(s2) * -1;
            }

            @Override
            public boolean equals(Object o)
            {
                return this.equals(o);
            }
        });

        for (File f : aFiles)
        {
            monthlyReports.addItemXref(contextPath + "/monthly-statistics/"
                    + (f.getName().substring(0, 6)),
                    (f.getName().substring(0, 6)));
        }
    }

    /**
     * Initialise the member variables from the request
     */
    private void initialise()
    {
        if (!initialised)
        {
            Request request = ObjectModelHelper.getRequest(objectModel);
            reportDate = request.getParameter("date");

            initialised = true;
        }
    }

    /**
     * Clear the member variables so that the instance can be reused
     */
    @Override
    public void recycle()
    {
        initialised = false;
        reportDate = null;
        validity = null;
        super.recycle();
    }
}
