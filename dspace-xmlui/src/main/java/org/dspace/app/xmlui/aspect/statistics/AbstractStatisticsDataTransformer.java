/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class AbstractStatisticsDataTransformer extends AbstractDSpaceTransformer {

    private static final Message T_time_filter_last_month = message("xmlui.statistics.StatisticsSearchTransformer.time-filter.last-month");
    private static final Message T_time_filter_overall = message("xmlui.statistics.StatisticsSearchTransformer.time-filter.overall");
    private static final Message T_time_filter_last_year = message("xmlui.statistics.StatisticsSearchTransformer.time-filter.last-year");
    private static final Message T_time_filter_last6_months = message("xmlui.statistics.StatisticsSearchTransformer.time-filter.last-6-months");


    protected void addTimeFilter(Division mainDivision) throws WingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String selectedTimeFilter = request.getParameter("time_filter");

        Select timeFilter = mainDivision.addPara().addSelect("time_filter");
        timeFilter.addOption(StringUtils.equals(selectedTimeFilter, "-1"), "-1", T_time_filter_last_month);
        timeFilter.addOption(StringUtils.equals(selectedTimeFilter, "-6"), "-6", T_time_filter_last6_months);
        timeFilter.addOption(StringUtils.equals(selectedTimeFilter, "-12"), "-12", T_time_filter_last_year);
        timeFilter.addOption(StringUtils.isBlank(selectedTimeFilter), "", T_time_filter_overall);
    }

    protected StatisticsSolrDateFilter getDateFilter(String timeFilter){
        if(StringUtils.isNotEmpty(timeFilter))
        {
            StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
            dateFilter.setStartStr(timeFilter);
            dateFilter.setEndStr("0");
            dateFilter.setTypeStr("month");
            return dateFilter;
        }else{
            return null;
        }
    }

    /**
     * Adds a table layout to the page
     *
     * @param mainDiv
     *            the div to add the table to
     * @param display the statistics table containing our data
     * @param addRowTitles switch.
     * @param valueMessagePrefixes for each column.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws java.text.ParseException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.apache.solr.client.solrj.SolrServerException passed through.
     * @throws java.sql.SQLException passed through.
     */
    protected void addDisplayTable(Division mainDiv, StatisticsTable display, boolean addRowTitles, String []valueMessagePrefixes)
            throws SAXException, WingException, SQLException,
            SolrServerException, IOException, ParseException {

        String title = display.getTitle();

        Dataset dataset = display.getDataset();

        if (dataset == null)
        {
            /** activate dataset query */
            dataset = display.getDataset(context);
        }

        if (dataset != null)
        {

            String[][] matrix = dataset.getMatrix();

            if(matrix.length == 0){
                //If no results are found alert the user of this !
                mainDiv.addPara(getNoResultsMessage());
                return;
            }

            /** Generate Table */
            Division wrapper = mainDiv.addDivision("tablewrapper");
            Table table = wrapper.addTable("list-table", 1, 1,
                    title == null ? "detailtable" : "tableWithTitle detailtable");
            if (title != null)
            {
                table.setHead(message(title));
            }

            /** Generate Header Row */
            Row headerRow = table.addRow();
            if(addRowTitles)
            {
                headerRow.addCell("spacer", Cell.ROLE_HEADER, "labelcell");
            }

            String[] cLabels = dataset.getColLabels().toArray(new String[0]);
            for (int row = 0; row < cLabels.length; row++)
            {
                Cell cell = headerRow.addCell(0 + "-" + row + "-h", Cell.ROLE_HEADER, "labelcell");
                cell.addContent(message("xmlui.statistics.display.table.column-label." + cLabels[row]));
            }

            /** Generate Table Body */
            for (int row = 0; row < matrix.length; row++) {
                Row valListRow = table.addRow();

                if(addRowTitles){
                    /** Add Row Title */
                    valListRow.addCell("" + row, Cell.ROLE_DATA, "labelcell")
                        .addContent(dataset.getRowLabels().get(row));
                }

                /** Add Rest of Row */
                for (int col = 0; col < matrix[row].length; col++) {
                    Cell cell = valListRow.addCell(row + "-" + col,
                            Cell.ROLE_DATA, "datacell");
                    String messagePrefix = null;
                    if(valueMessagePrefixes != null && col < valueMessagePrefixes.length){
                        messagePrefix = valueMessagePrefixes[col];
                    }

                    if(messagePrefix != null){
                        cell.addContent(message(messagePrefix + matrix[row][col]));
                    }else{
                        cell.addContent(matrix[row][col]);
                    }
                }
            }
        }
    }

    protected abstract Message getNoResultsMessage();
}
