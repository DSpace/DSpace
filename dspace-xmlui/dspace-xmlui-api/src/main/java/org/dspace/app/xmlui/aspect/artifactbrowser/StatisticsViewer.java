/*
 * Statistics.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2007/08/28 10:00:00 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.statistics.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.*;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Transformer to display statistics data in XML UI.
 *
 * Unlike the JSP interface that pre-generates HTML and stores in the reports folder,
 * this class transforms the raw analysis data into a Wing representation
 */
public class StatisticsViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private final static Logger log = Logger.getLogger(StatisticsViewer.class);

    private final static Message T_dspace_home = message("xmlui.general.dspace_home");

    private final static Message T_choose_report = message("xmlui.ArtifactBrowser.StatisticsViewer.choose_month");
    private final static Message T_page_title    = message("xmlui.ArtifactBrowser.StatisticsViewer.report.title");

    private final static Message T_empty_title   = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.title");
    private final static Message T_empty_text    = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.text");

    private final static SimpleDateFormat sdfDisplay = new SimpleDateFormat("MM'/'yyyy");
    private final static SimpleDateFormat sdfLink    = new SimpleDateFormat("yyyy'-'M");

    private boolean initialised = false;
    private String reportDate = null;
    private SourceValidity validity;

    /**
     * Get the caching key for this report
     * @return
     */
    public Serializable getKey()
    {
        initialise();

        if (reportDate != null)
            return reportDate;

        return "general";
    }

    /**
     * Generate the validity for this cached entry
     * @return
     */
    public SourceValidity getValidity()
    {
        if (validity == null)
        {
            try
            {
                initialise();
                boolean showReport = ConfigurationManager.getBooleanProperty("report.public");

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
                        analysisFile = StatisticsLoader.getAnalysisFor(reportDate);
                    else
                        analysisFile = StatisticsLoader.getGeneralAnalysis();

                    if (analysisFile != null)
                    {
                        // Generate the validity based on the properties of the report data file
                        DSpaceValidity newValidity = new DSpaceValidity();
                        newValidity.add(Long.toString(analysisFile.lastModified()));
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
     * Add additional navigation options. This is to allow selection of a monthly report
     * 
     * @param options
     * @throws SAXException
     * @throws WingException
     * @throws UIException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Date[] monthlyDates = StatisticsLoader.getMonthlyAnalysisDates();

        if (monthlyDates != null && monthlyDates.length > 0)
        {
            List statList = options.addList("statsreports");
            statList.setHead(T_choose_report);

            HashMap<String, String> params = new HashMap<String, String>();
            for (Date date : monthlyDates)
            {
                params.put("date", sdfLink.format(date));
                statList.addItemXref(super.generateURL("statistics", params), sdfDisplay.format(date));
            }
        }
    }

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
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException,
                                                      SQLException, IOException, AuthorizeException
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
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException,
            IOException, AuthorizeException
    {
        initialise();
        boolean publicise = ConfigurationManager.getBooleanProperty("report.public");

        // Check that the reports are either public, or user is an administrator
        if (!publicise && !AuthorizeManager.isAdmin(context))
            throw new AuthorizeException();

        // Retrieve the report data to display
        File analysisFile;
        if (reportDate != null)
            analysisFile = StatisticsLoader.getAnalysisFor(reportDate);
        else
            analysisFile = StatisticsLoader.getGeneralAnalysis();

        // Create the renderer for the results
        Division div = body.addDivision("statistics", "primary");

        if (analysisFile != null)
        {
            try
            {
                // Generate the XML stream
                Report myRep = new XMLUIReport(div);
                ReportGenerator.processReport(context, myRep, analysisFile.getCanonicalPath());
            }
            catch (Exception e)
            {
                throw new UIException(e);
            }
        }
        else
        {
            div.setHead(T_empty_title);
            div.addPara(T_empty_text);
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
            reportDate = (String) request.getParameter("date");

            initialised = true;
        }
    }

    /**
     * Clear the member variables so that the instance can be reused
     */
    public void recycle()
    {
        initialised = false;
        reportDate = null;
        validity = null;
        super.recycle();
    }

    /**
     * Implementation of the Report interface, to output the statistics data for xmlui
     * Note that all methods that return Strings return 'null' in this implementation, as
     * all the outputting is done directly using the Wing framework.
     */
    class XMLUIReport implements Report
    {
        private ArrayList<Statistics> blocks = new ArrayList<Statistics>();

        private String mainTitle = null;
        private String pageTitle = null;

        /** start date for report */
        private Date start = null;

        /** end date for report */
        private Date end = null;

        private Division rootDiv;
        private Division currDiv;

        /**
         * Hide the default constructor, so that you have to pass in a Division
         */
        private XMLUIReport() {}

        /**
         * Create instance, providing the Wing element that we will be adding to
         *
         * @param myDiv
         */
        public  XMLUIReport(Division myDiv)
        {
            rootDiv = myDiv;
            currDiv = myDiv;
        }

        /**
         * Get the header for the report - currently not supported
         *
         * @return
         */
        public String header()
        {
            return header("");
        }

        // Currently not supported
        public String header(String title)
        {
            return "";
        }

        /**
         * Add the main title to the report
         * @return
         */
        public String mainTitle()
        {
            try
            {
                rootDiv.setHead(mainTitle);
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }
            return null;
        }

        /**
         * Output the date range for this report
         * @return
         */
        public String dateRange()
        {
            StringBuilder content = new StringBuilder();
            DateFormat df = DateFormat.getDateInstance();
            if (start != null)
            {
                content.append(df.format(start));
            }
            else
            {
                content.append("from start of records ");
            }

            content.append(" to ");

            if (end != null)
            {
                content.append(df.format(end));
            }
            else
            {
                content.append(" end of records");
            }

            try
            {
                rootDiv.addDivision("reportDate").addPara(content.toString());
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }

            return null;
        }

        /**
         * Output the section header
         * @param title
         * @return
         */
        public String sectionHeader(String title)
        {
            try
            {
                currDiv.setHead(title);
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }

            return null;
        }

        /**
         * Output the current statistics block
         * @param content
         * @return
         */
        public String statBlock(Statistics content)
        {
            Stat[] stats = content.getStats();
            try
            {
                int rows = stats.length;
                if (content.getStatName() != null || content.getResultName() != null)
                    rows++;

                Table block = currDiv.addTable("reportBlock", rows, 2);

                // prepare the table headers
                if (content.getStatName() != null || content.getResultName() != null)
                {
                    Row row = block.addRow();
                    if (content.getStatName() != null)
                    {
                        row.addCellContent(content.getStatName());
                    }
                    else
                    {
                        row.addCellContent("&nbsp;");
                    }

                    if (content.getResultName() != null)
                    {
                        row.addCellContent(content.getResultName());
                    }
                    else
                    {
                        row.addCellContent("&nbsp;");
                    }
                }

                // output the statistics in the table
                for (int i = 0; i < stats.length; i++)
                {
                    Row row = block.addRow();
                    if (stats[i].getReference() != null)
                    {
                        row.addCell().addXref(stats[i].getReference()).addContent(stats[i].getKey());
                    }
                    else
                    {
                        row.addCell().addContent(stats[i].getKey());
                    }

                    if (stats[i].getUnits() != null)
                    {
                        row.addCell(null, null, "right").addContent(stats[i].getValue() + " " + stats[i].getUnits());
                    }
                    else
                    {
                        row.addCell(null, null, "right").addContent(ReportTools.numberFormat(stats[i].getValue()));
                    }
                }
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }

            return null;
        }

        /**
         * Output any information about the lower boundary restriction for this section
         * @param floor
         * @return
         */
        public String floorInfo(int floor)
        {
            try
            {
                if (floor > 0)
                {
                    currDiv.addDivision("reportFloor").addPara("(more than " + ReportTools.numberFormat(floor) + " times)");
                }
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }

            return null;
        }

        /**
         * Output an explanation for this section
         * 
         * @param explanation
         * @return
         */
        public String blockExplanation(String explanation)
        {
            try
            {
                if (explanation != null)
                {
                    currDiv.addDivision("reportExplanation").addPara(explanation);
                }
            }
            catch (WingException we)
            {
                log.error("Error creating XML for report", we);
            }

            return null;
        }

        /**
         * Output the footer
         * 
         * @return
         */
        public String footer()
        {
            return "";
        }

        /**
         * Set the main title for this report
         * 
         * @param name
         * @param serverName
         */
        public void setMainTitle(String name, String serverName)
        {
            mainTitle = "Statistics for " + name;

            if (ConfigurationManager.getBooleanProperty("report.show.server", true))
                mainTitle += " on " + serverName;
            
            if (pageTitle == null)
            {
                pageTitle = mainTitle;
            }
            return;
        }

        /**
         * Add a block to report on
         *
         * @param stat
         */
        public void addBlock(Statistics stat)
        {
            blocks.add(stat);
            return;
        }

        /**
         * Render the statistics into an XML stream
         * @return
         */
        public String render()
        {
            Pattern space = Pattern.compile(" ");

            // Output the heading information
            header(pageTitle);
            mainTitle();
            dateRange();

            // Loop through all the sections
            for (Statistics stats : blocks)
            {
                // navigation();
                try
                {
                    String title = stats.getSectionHeader();
                    String aName = title.toLowerCase();
                    Matcher matchSpace = space.matcher(aName);
                    aName = matchSpace.replaceAll("_");

                    // Create a new division for each section
                    currDiv = rootDiv.addDivision(aName);
                    sectionHeader(title);
                    // topLink();
                    blockExplanation(stats.getExplanation());
                    floorInfo(stats.getFloor());
                    statBlock(stats);
                    currDiv = rootDiv;
                }
                catch (WingException we)
                {
                    log.error("Error creating XML for report", we);
                }
            }

            return null;
        }

        /**
         * Set the start date for this report
         * @param start
         */
        public void setStartDate(Date start)
        {
            this.start = start;
        }

        /**
         * Set the end date for this report
         * @param end
         */
        public void setEndDate(Date end)
        {
            this.end = end;
        }
    }
}
