/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.statistics.Report;
import org.dspace.app.statistics.ReportTools;
import org.dspace.app.statistics.Stat;
import org.dspace.app.statistics.Statistics;
import org.dspace.app.statistics.StatisticsLoader;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Transformer to display statistics data in XML UI.
 *
 * Unlike the JSP interface that pre-generates HTML and stores in the reports
 * folder, this class transforms the raw analysis data into a Wing
 * representation.
 */
public class EmbargoListDisplay extends AbstractDSpaceTransformer implements
CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(StatisticsViewer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_choose_report = message("xmlui.ArtifactBrowser.StatisticsViewer.choose_month");

    private static final Message T_page_title = message("xmlui.ArtifactBrowser.EmbargoList.title");

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
                params.put("date", sdfLink.format(date));
                statList.addItemXref(super.generateURL("statistics", params),
                        sdfDisplay.format(date));
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
        Division div = body.addDivision("embargo-list", "primary");

        List download = div.addList("download");
        download.addItemXref(contextPath + "/embargo-list/csv", "Download CSV");

        String sql = "   SELECT" + "   DISTINCT ON (h.handle) h.handle,"
                + "   i1.item_id," + "   bs.bitstream_id," + "   (SELECT"
                + "      dc.text_value " + "    FROM "
                + "      metadatavalue dc" + "    WHERE "
                + "      dc.metadata_field_id='64' AND"
                + "      dc.item_id=i1.item_id" + "    LIMIT 1) as title,"
                + "   (SELECT" + "      dc.text_value " + "    FROM "
                + "      metadatavalue dc" + "    WHERE "
                + "      dc.metadata_field_id='2' AND"
                + "      dc.item_id=i1.item_id" + "    LIMIT 1) as advisor,"
                + "   (SELECT" + "       dc.text_value" + "    FROM"
                + "       metadatavalue dc" + "    WHERE"
                + "       dc.metadata_field_id='3' AND"
                + "       dc.item_id=i1.item_id" + "    LIMIT 1) as author,"
                + "   (SELECT" + "       dc.text_value" + "    FROM"
                + "       metadatavalue dc" + "    WHERE"
                + "       dc.metadata_field_id='69' AND"
                + "       dc.item_id=i1.item_id"
                + "    LIMIT 1) as department," + "   (SELECT"
                + "       dc.text_value" + "    FROM"
                + "       metadatavalue dc" + "    WHERE"
                + "       dc.metadata_field_id='66' AND"
                + "       dc.item_id=i1.item_id" + "    LIMIT 1) as type,"
                + "   rp.end_date" + " FROM" + "   handle h," + "   item i1, "
                + "   item2bundle i2b1," + "   bundle2bitstream b2b1, "
                + "   bitstream bs, " + "   resourcepolicy rp, "
                + "   epersongroup g " + " WHERE "
                + "   h.resource_id=i1.item_id AND"
                + "   i1.item_id=i2b1.item_id AND"
                + "   i2b1.bundle_id=b2b1.bundle_id AND"
                + "   b2b1.bitstream_id=bs.bitstream_id AND"
                + "   bs.bitstream_id=rp.resource_id AND"
                + "   (rp.end_date > CURRENT_DATE OR"
                + "   rp.end_date IS NULL) AND"
                + "   rp.epersongroup_id = g.eperson_group_id AND"
                + "   g.name = 'ETD Embargo'";

        Request request = ObjectModelHelper.getRequest(objectModel);
        Response response = ObjectModelHelper.getResponse(objectModel);
        CSVWriter writer = null;

        Context context = UIUtil.obtainContext(request);

        TableRowIterator tri = DatabaseManager.query(context, sql);
        ArrayList<TableRow> rowList = (ArrayList<TableRow>) tri.toList();

        String strDownload = request.getParameter("download");
        if (strDownload.compareTo("csv") == 0)
        {
            response.setContentType("text/csv; encoding='UTF-8'");
            response.setStatus(HttpServletResponse.SC_OK);
            writer = new CSVWriter(response.getWriter());
            response.setHeader("Content-Disposition",
                    "attachment; filename=embargo-statistics.csv");
            writer.writeNext(new String[] { "Handle", "Item ID",
                    "Bitstream ID", "Title", "Advisor", "Author", "Department",
                    "Type", "End Date" });

            for (TableRow row : rowList)
            {
                String entryData[] = new String[9];
                entryData[0] = row.getStringColumn("handle");
                entryData[1] = String.valueOf(row.getIntColumn("item_id"));
                entryData[2] = String.valueOf(row.getIntColumn("bitstream_id"));
                entryData[3] = row.getStringColumn("title");
                entryData[4] = row.getStringColumn("advisor");
                entryData[5] = row.getStringColumn("author");
                entryData[6] = row.getStringColumn("department");
                entryData[7] = row.getStringColumn("type");
                entryData[8] = row.getDateColumn("end_date").toString();
                writer.writeNext(entryData);
            }
            writer.close();

        }

        else
        {
            Table table = div.addTable("list-of-embargoes", 22, 9);
            div.addPara(String.valueOf(rowList.size()));

            Row header = table.addRow(Row.ROLE_HEADER);

            header.addCell().addContent("Handle");
            header.addCell().addContent("Item ID");
            header.addCell().addContent("Bitstream ID");
            header.addCell().addContent("Title");
            header.addCell().addContent("Advisor");
            header.addCell().addContent("Author");
            header.addCell().addContent("Department");
            header.addCell().addContent("Type");
            header.addCell().addContent("End Date");

            // for (TableRow row : rowList) {
            for (int i = 0; i < 20; i++)
            {
                TableRow row = rowList.get(i);

                Row tableRow = table.addRow();

                tableRow.addCell().addContent(row.getStringColumn("handle"));
                tableRow.addCell().addContent(row.getIntColumn("item_id"));
                tableRow.addCell().addContent(row.getIntColumn("bitstream_id"));
                tableRow.addCell().addContent(row.getStringColumn("title"));
                tableRow.addCell().addContent(row.getStringColumn("advisor"));
                tableRow.addCell().addContent(row.getStringColumn("author"));
                tableRow.addCell()
                .addContent(row.getStringColumn("department"));
                tableRow.addCell().addContent(row.getStringColumn("type"));
                tableRow.addCell().addContent(
                        row.getDateColumn("end_date").toString());

            }
            tri.close();
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

    /**
     * Implementation of the Report interface, to output the statistics data for
     * xmlui Note that all methods that return Strings return 'null' in this
     * implementation, as all the outputting is done directly using the Wing
     * framework.
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
        private XMLUIReport()
        {
        }

        /**
         * Create instance, providing the Wing element that we will be adding to
         *
         * @param myDiv
         */
        public XMLUIReport(Division myDiv)
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
         *
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
         *
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
         *
         * @param title
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
         *
         * @param content
         * @return null.
         */
        @Override
        public String statBlock(Statistics content)
        {
            Stat[] stats = content.getStats();
            try
            {
                int rows = stats.length;
                if (content.getStatName() != null
                        || content.getResultName() != null)
                {
                    rows++;
                }

                Table block = currDiv.addTable("reportBlock", rows, 2,
                        "detailtable");

                // prepare the table headers
                if (content.getStatName() != null
                        || content.getResultName() != null)
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
                        row.addCell().addXref(stats[i].getReference())
                        .addContent(label(stats[i].getKey()));
                    }
                    else
                    {
                        row.addCell().addContent(label(stats[i].getKey()));
                    }

                    if (stats[i].getUnits() != null)
                    {
                        row.addCell(null, null, "right").addContent(
                                entry(stats[i].getValue() + " "
                                        + stats[i].getUnits()));
                    }
                    else
                    {
                        row.addCell(null, null, "right").addContent(
                                entry(ReportTools.numberFormat(stats[i]
                                        .getValue())));
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
         * Output any information about the lower boundary restriction for this
         * section.
         *
         * @param floor
         * @return null.
         */
        @Override
        public String floorInfo(int floor)
        {
            try
            {
                if (floor > 0)
                {
                    currDiv.addDivision("reportFloor").addPara(
                            "(more than " + ReportTools.numberFormat(floor)
                            + " times)");
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
         * @param explanation
         * @return null.
         */
        @Override
        public String blockExplanation(String explanation)
        {
            try
            {
                if (explanation != null)
                {
                    currDiv.addDivision("reportExplanation").addPara(
                            explanation);
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
         * @param name
         * @param serverName
         */
        @Override
        public void setMainTitle(String name, String serverName)
        {
            mainTitle = "Statistics for " + name;

            if (ConfigurationManager.getBooleanProperty("report.show.server",
                    true))
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
         * Add a block to report on
         *
         * @param stat
         */
        @Override
        public void addBlock(Statistics stat)
        {
            blocks.add(stat);
            return;
        }

        /**
         * Render the statistics into an XML stream.
         *
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
         * Set the start date for this report
         *
         * @param start
         */
        @Override
        public void setStartDate(Date start)
        {
            this.start = start;
        }

        /**
         * Set the end date for this report
         *
         * @param end
         */
        @Override
        public void setEndDate(Date end)
        {
            this.end = end;
        }
    }

    /**
     * Protect the display from excessively wrong data. Typically this occurs if
     * a long word finds its way into the data that is not breakable by the
     * browser because there is no space, dash, period, or other delimiter
     * character. This just prevents the page from blowing up when bad data are
     * being presented.
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
