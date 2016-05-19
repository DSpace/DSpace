/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

/**
 * Transformer to display statistics data in XML UI.
 *
 * Unlike the JSP interface that pre-generates HTML and stores in the reports
 * folder, this class transforms the raw analysis data into a Wing
 * representation.
 */
public class StatisticsViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(StatisticsViewer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_choose_report = message("xmlui.ArtifactBrowser.StatisticsViewer.choose_month");
    private static final Message T_page_title    = message("xmlui.ArtifactBrowser.StatisticsViewer.report.title");

    private static final Message T_empty_title   = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.title");
    private static final Message T_empty_text    = message("xmlui.ArtifactBrowser.StatisticsViewer.no_report.text");

    private static final ThreadLocal<DateFormat> sdfDisplay = new ThreadLocal<DateFormat>(){
                @Override
                protected DateFormat initialValue() {
                    return new SimpleDateFormat("MM'/'yyyy");
                }
              };
    private static final ThreadLocal<DateFormat> sdfLink    = new ThreadLocal<DateFormat>(){
                @Override
                protected DateFormat initialValue() {
                    return new SimpleDateFormat("yyyy'-'M");
                }
              };

    private boolean initialised = false;
    private String reportDate = null;
    private SourceValidity validity;


    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    /**
     * Get the caching key for this report.
     * @return the key.
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
     * @return the validity.
     */
    @Override
    public SourceValidity getValidity()
    {
        if (validity == null)
        {
            try
            {
                initialise();
                boolean showReport = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("report.public");

                // If the report isn't public
                if (!showReport)
                {
                    try
                    {
                        // Administrators can always view reports
                        showReport = authorizeService.isAdmin(context);
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
                        analysisFile = StatisticsLoader.getAnalysisFor(reportDate);
                    }
                    else
                    {
                        analysisFile = StatisticsLoader.getGeneralAnalysis();
                    }

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
     * Add additional navigation options. This is to allow selection of a monthly report.
     *
     * @param options new options.
     * @throws SAXException passed through.
     * @throws WingException passed through.
     * @throws UIException passed through.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     */
    @Override
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
                params.put("date", sdfLink.get().format(date));
                statList.addItemXref(super.generateURL("statistics", params), sdfDisplay.get().format(date));
            }
        }
    }

    /**
     * Add title, etc. metadata.
     *
     * @param pageMeta new metadata.
     * @throws SAXException passed through.
     * @throws WingException passed through.
     * @throws UIException passed through.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException,
                                                      SQLException, IOException, AuthorizeException
    {
        initialise();

        pageMeta.addMetadata("title").addContent(T_page_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_page_title);
    }

    /**
     * Output the body of the report.
     * 
     * @param body the body.
     * @throws SAXException passed through.
     * @throws WingException passed through.
     * @throws UIException passed through.
     * @throws SQLException passed through.
     * @throws IOException passed through.
     * @throws AuthorizeException passed through.
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException,
            IOException, AuthorizeException
    {
        initialise();
        boolean publicise = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("report.public");

        // Check that the reports are either public, or user is an administrator
        if (!publicise && !authorizeService.isAdmin(context))
        {
            throw new AuthorizeException();
        }

        // Retrieve the report data to display
        File analysisFile;
        if (reportDate != null)
        {
            analysisFile = StatisticsLoader.getAnalysisFor(reportDate);
        }
        else
        {
            analysisFile = StatisticsLoader.getGeneralAnalysis();
        }

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
     * Initialise the member variables from the request.
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
     * Clear the member variables so that the instance can be reused.
     */
    @Override
    public void recycle()
    {
        initialised = false;
        reportDate = null;
        validity = null;
        super.recycle();
    }

    /**
     * Implementation of the Report interface, to output the statistics data for XMLUI.
     * Note that all methods that return Strings return 'null' in this implementation, as
     * all the outputting is done directly using the Wing framework.
     */
    static class XMLUIReport implements Report
    {
        private java.util.List<Statistics> blocks = new ArrayList<Statistics>();

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
         * Get the header for the report - currently not supported.
         */
        @Override
        public String header()
        {
            return header("");
        }

        // Currently not supported
        @Override
        public String header(String title)
        {
            return "";
        }

        /**
         * Add the main title to the report.
         * @return null.
         */
        @Override
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
         * Output the date range for this report.
         * @return null.
         */
        @Override
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
         * Output the section header.
         * @param title the title of the section.
         * @return null.
         */
        @Override
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
         * Output the current statistics block.
         * @param content the current statistics.
         * @return null.
         */
        @Override
        public String statBlock(Statistics content)
        {
            Stat[] stats = content.getStats();
            try
            {
                int rows = stats.length;
                if (content.getStatName() != null || content.getResultName() != null)
                {
                    rows++;
                }

                Table block = currDiv.addTable("reportBlock", rows, 2, "detailtable");

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
                        row.addCell().addXref(stats[i].getReference()).addContent(label(stats[i].getKey()));
                    }
                    else
                    {
                        row.addCell().addContent(label(stats[i].getKey()));
                    }

                    if (stats[i].getUnits() != null)
                    {
                        row.addCell(null, null, "right").addContent(entry(stats[i].getValue() + " " + stats[i].getUnits()));
                    }
                    else
                    {
                        row.addCell(null, null, "right").addContent(entry(ReportTools.numberFormat(stats[i].getValue())));
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
         * Output any information about the lower boundary restriction for
         * this section.
         * @param floor boundary.
         * @return null.
         */
        @Override
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
         * Output an explanation for this section.
         * 
         * @param explanation the explanation.
         * @return null.
         */
        @Override
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
         * Output the footer.
         * 
         * @return an empty string.
         */
        @Override
        public String footer()
        {
            return "";
        }

        /**
         * Set the main title for this report
         * 
         * @param name instance name.
         * @param serverName name of the server.
         */
        @Override
        public void setMainTitle(String name, String serverName)
        {
            mainTitle = "Statistics for " + name;

            if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("report.show.server", true))
            {
                mainTitle += " on " + serverName;
            }
            
            if (pageTitle == null)
            {
                pageTitle = mainTitle;
            }
            return;
        }

        /**
         * Add a block to report on.
         *
         * @param stat the block to add.
         */
        @Override
        public void addBlock(Statistics stat)
        {
            blocks.add(stat);
            return;
        }

        /**
         * Render the statistics into an XML stream.
         * @return null.
         */
        @Override
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
         * Set the start date for this report.
         * @param start the date.
         */
        @Override
        public void setStartDate(Date start)
        {
            this.start = start;
        }

        /**
         * Set the end date for this report.
         * @param end the end.
         */
        @Override
        public void setEndDate(Date end)
        {
            this.end = end;
        }
    }
    
    
    /**
     * Protect the display from excessively wrong data.  Typically this occurs
     * if a long word finds its way into the data that is not breakable by
     * the browser because there is no space, dash, period, or other 
     * delimiter character. This just prevents the page from blowing up when
     * bad data are being presented.
     */
    private static final int MAX_ENTRY_LENGTH = 50;
    private static String entry(String entry) 
    {
    	if (entry != null && entry.length() > MAX_ENTRY_LENGTH)
        {
            entry = entry.substring(0, MAX_ENTRY_LENGTH - 3) + "...";
        }
    	return entry;
    }
    
    private static final int MAX_LABEL_LENGTH = 100;
    private static String label(String label) 
    {
    	if (label != null && label.length() > MAX_LABEL_LENGTH)
        {
            label = label.substring(0, MAX_LABEL_LENGTH - 3) + "...";
        }
    	return label;
    }
}
