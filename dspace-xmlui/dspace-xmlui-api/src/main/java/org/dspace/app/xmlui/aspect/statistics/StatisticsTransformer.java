/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.*;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StatisticsTransformer extends AbstractDSpaceTransformer {

	private static Logger log = Logger.getLogger(StatisticsTransformer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statistics.title");
    private static final Message T_statistics_trail = message("xmlui.statistics.trail");
    private static final String T_head_visits_total = "xmlui.statistics.visits.total";
    private static final String T_head_visits_month = "xmlui.statistics.visits.month";
    private static final String T_head_visits_views = "xmlui.statistics.visits.views";
    private static final String T_head_visits_countries = "xmlui.statistics.visits.countries";
    private static final String T_head_visits_cities = "xmlui.statistics.visits.cities";
    private static final String T_head_visits_bitstream = "xmlui.statistics.visits.bitstreams";

    private Date dateStart = null;
    private Date dateEnd = null;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public StatisticsTransformer(Date dateStart, Date dateEnd) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

        try {
            this.context = new Context();
        } catch (SQLException e) {
            log.error("Error getting context in StatisticsTransformer:" + e.getMessage());
        }
    }

    public StatisticsTransformer() {
        try {
            this.context = new Context();
        } catch (SQLException e) {
            log.error("Error getting context in StatisticsTransformer:" + e.getMessage());
        }
    }

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        if(dso != null)
        {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath, true);
        }
        pageMeta.addTrailLink(contextPath + "/handle" + (dso != null && dso.getHandle() != null ? "/" + dso.getHandle() : "/statistics"), T_statistics_trail);

        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
    }

    /**
	 * What to add at the end of the body
	 */
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

		try
		{
			if(dso != null)
			{
				renderViewer(body, dso);
			}
			else
			{
				renderHome(body);
			}

        } catch (RuntimeException e) {
            throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public void renderHome(Body body) throws WingException {
		
		Division home = body.addDivision("home", "primary repository");
		Division division = home.addDivision("stats", "secondary stats");
		division.setHead(T_head_title);
        /*
		try {

			StatisticsTable statisticsTable = new StatisticsTable(
					new StatisticsDataVisits());

			statisticsTable.setTitle(T_head_visits_month);
			statisticsTable.setId("tab1");

			DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
			timeAxis.setDateInterval("month", "-6", "+1");
			statisticsTable.addDatasetGenerator(timeAxis);

			addDisplayTable(division, statisticsTable);

		} catch (Exception e) {
			log.error("Error occurred while creating statistics for home page",
					e);
		}
		*/
		try {
            /** List of the top 10 items for the entire repository **/
			StatisticsListing statListing = new StatisticsListing(
					new StatisticsDataVisits());

			statListing.setTitle(T_head_visits_total);
			statListing.setId("list1");

            //Adding a new generator for our top 10 items without a name length delimiter
            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(Constants.ITEM, 10, false, -1);
            statListing.addDatasetGenerator(dsoAxis);

            //Render the list as a table
			addDisplayListing(division, statListing);

		} catch (Exception e) {
			log.error("Error occurred while creating statistics for home page", e);
		}

	}

	public void renderViewer(Body body, DSpaceObject dso) throws WingException {

		Division home = body.addDivision(
				Constants.typeText[dso.getType()].toLowerCase() + "-home", 
				"primary repository " + Constants.typeText[dso.getType()].toLowerCase());
		
		// Build the collection viewer division.
		Division division = home.addDivision("stats", "secondary stats");
		division.setHead(T_head_title);

		
		try {
			StatisticsListing statListing = new StatisticsListing(
					new StatisticsDataVisits(dso));

			statListing.setTitle(T_head_visits_total);
			statListing.setId("list1");

			DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
			dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
			statListing.addDatasetGenerator(dsoAxis);

			addDisplayListing(division, statListing);

		} catch (Exception e) {
			log.error(
					"Error occurred while creating statistics for dso with ID: "
							+ dso.getID() + " and type " + dso.getType()
							+ " and handle: " + dso.getHandle(), e);
		}
		
		
		
		try {

			StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

			statisticsTable.setTitle(T_head_visits_month);
			statisticsTable.setId("tab1");

			DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
			timeAxis.setDateInterval("month", "-6", "+1");
			statisticsTable.addDatasetGenerator(timeAxis);

			DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
			dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
			statisticsTable.addDatasetGenerator(dsoAxis);

			addDisplayTable(division, statisticsTable);

		} catch (Exception e) {
			log.error(
					"Error occurred while creating statistics for dso with ID: "
							+ dso.getID() + " and type " + dso.getType()
							+ " and handle: " + dso.getHandle(), e);
		}

         if(dso instanceof org.dspace.content.Item){
             //Make sure our item has at least one bitstream
             org.dspace.content.Item item = (org.dspace.content.Item) dso;
            try {
                if(item.hasUploadedFiles()){
                    StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));

                    statsList.setTitle(T_head_visits_bitstream);
                    statsList.setId("list-bit");

                    DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                    dsoAxis.addDsoChild(Constants.BITSTREAM, 10, false, -1);
                    statsList.addDatasetGenerator(dsoAxis);

                    addDisplayListing(division, statsList);
                }
            } catch (Exception e) {
                log.error(
                        "Error occurred while creating statistics for dso with ID: "
                                + dso.getID() + " and type " + dso.getType()
                                + " and handle: " + dso.getHandle(), e);
            }
        }

        try {
            StatisticsListing statListing = new StatisticsListing(
                       new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_countries);
            statListing.setId("list2");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("countryCode");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating statistics for dso with ID: "
                            + dso.getID() + " and type " + dso.getType()
                            + " and handle: " + dso.getHandle(), e);
        }

        try {
            StatisticsListing statListing = new StatisticsListing(
                       new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_cities);
            statListing.setId("list3");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("city");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating statistics for dso with ID: "
                            + dso.getID() + " and type " + dso.getType()
                            + " and handle: " + dso.getHandle(), e);
        }

	}

	
	/**
	 * Adds a table layout to the page
	 * 
	 * @param mainDiv
	 *            the div to add the table to
	 * @param display
	 * @throws SAXException
	 * @throws WingException
	 * @throws ParseException
	 * @throws IOException
	 * @throws SolrServerException
	 * @throws SQLException
	 */
	private void addDisplayTable(Division mainDiv, StatisticsTable display)
			throws SAXException, WingException, SQLException,
			SolrServerException, IOException, ParseException {

		String title = display.getTitle();

		Dataset dataset = display.getDataset();

		if (dataset == null) {
			/** activate dataset query */
			dataset = display.getDataset(context);
		}

		if (dataset != null) {

			String[][] matrix = dataset.getMatrixFormatted();

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
			headerRow.addCell("spacer", Cell.ROLE_DATA, "labelcell");

			String[] cLabels = dataset.getColLabels().toArray(new String[0]);
			for (int row = 0; row < cLabels.length; row++) {
				Cell cell = headerRow.addCell(0 + "-" + row + "-h",
						Cell.ROLE_DATA, "labelcell");
				cell.addContent(cLabels[row]);
			}

			/** Generate Table Body */
			for (int row = 0; row < matrix.length; row++) {
				Row valListRow = table.addRow();

				/** Add Row Title */
				valListRow.addCell("" + row, Cell.ROLE_DATA, "labelcell")
						.addContent(dataset.getRowLabels().get(row));

				/** Add Rest of Row */
				for (int col = 0; col < matrix[row].length; col++) {
					Cell cell = valListRow.addCell(row + "-" + col,
							Cell.ROLE_DATA, "datacell");
					cell.addContent(matrix[row][col]);
				}
			}
		}

	}

	private void addDisplayListing(Division mainDiv, StatisticsListing display)
			throws SAXException, WingException, SQLException,
			SolrServerException, IOException, ParseException {

		String title = display.getTitle();

		Dataset dataset = display.getDataset();

		if (dataset == null) {
			/** activate dataset query */
			dataset = display.getDataset(context);
		}

		if (dataset != null) {

			String[][] matrix = dataset.getMatrixFormatted();

			// String[] rLabels = dataset.getRowLabels().toArray(new String[0]);

			Table table = mainDiv.addTable("list-table", matrix.length, 2,
					title == null ? "detailtable" : "tableWithTitle detailtable");
			if (title != null)
            {
                table.setHead(message(title));
            }

			Row headerRow = table.addRow();

			headerRow.addCell("", Cell.ROLE_DATA, "labelcell");
			
			headerRow.addCell("", Cell.ROLE_DATA, "labelcell").addContent(message(T_head_visits_views));

			/** Generate Table Body */
			for (int col = 0; col < matrix[0].length; col++) {
				Row valListRow = table.addRow();

				Cell catCell = valListRow.addCell(col + "1", Cell.ROLE_DATA,
						"labelcell");
				catCell.addContent(dataset.getColLabels().get(col));

				Cell valCell = valListRow.addCell(col + "2", Cell.ROLE_DATA,
						"datacell");
				valCell.addContent(matrix[0][col]);

			}

			if (!"".equals(display.getCss())) {
				List attrlist = mainDiv.addList("divattrs");
				attrlist.addItem("style", display.getCss());
			}

		}

	}

    public java.util.List<TableRow> addItemsInContainer(DSpaceObject dso) {
        java.util.List<TableRow> tableRowList = null;

        String typeTextLower = "";
        if(dso.getType() == Constants.COMMUNITY) {
            typeTextLower = "communities";
        } else {
            typeTextLower = dso.getTypeText().toLowerCase();
        }

        String querySpecifyContainer = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item, " +
                typeTextLower + "2item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true AND "+
                typeTextLower + "2item.item_id = item.item_id AND "+
                typeTextLower + "2item." + dso.getTypeText().toLowerCase() +"_id = ? ";

        if (dateStart != null) {
            String start = dateFormat.format(dateStart);
            querySpecifyContainer += "AND metadatavalue.text_value > '"+start+"'";
        }
        if(dateEnd != null) {
            String end = dateFormat.format(dateEnd);
            querySpecifyContainer += " AND metadatavalue.text_value < '"+end+"' ";
        }

        querySpecifyContainer += ") t1 GROUP BY date_trunc('month', t1.ts) order by yearmo asc";

        try {
            TableRowIterator tri;
            tri = DatabaseManager.query(context, querySpecifyContainer, dso.getID());

            tableRowList = tri.toList();
            return tableRowList;
        } catch (Exception e) {

        }
        return tableRowList;
    }

    /**
     * Only call this on a container object (collection or community).
     * @param dso
     * @param division
     */
    public void addItemsInContainer(DSpaceObject dso, Division division) {
        java.util.List<TableRow> tableRowList = addItemsInContainer(dso);

        Gson gson = new Gson();
        try {
            division.addHidden("gson-itemsAdded").setValue(gson.toJson(tableRowList));

            Integer[][] monthlyDataGrid = convertTableRowListToIntegerGrid(tableRowList, "yearmo", "countitem");
            displayAsGrid(division, monthlyDataGrid, "itemsAddedGrid", "Number of Items Added to the " + dso.getName());
        } catch (WingException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public Integer[][] convertTableRowListToIntegerGrid(java.util.List<TableRow> tableRowList, String dateColumn, String valueColumn) {
        if(tableRowList == null || tableRowList.size() == 0) {
            return null;
        }

        // If we have a requested time-range for data, then we need to ensure we zero-fill the data during that time range.
        // Otherwise, if no requested time-range, then just make table size of data.
        // a year-mo is 2008-05
        Integer yearStart;
        Integer yearLast;

        if(dateStart != null) {
            Calendar myTime = Calendar.getInstance();
            myTime.setTime(dateStart);
            yearStart = myTime.get(Calendar.YEAR);
        } else {
            String yearmoStart = tableRowList.get(0).getStringColumn(dateColumn);
            yearStart = Integer.valueOf(yearmoStart.split("-")[0]);
        }

        if(dateEnd != null) {
            Calendar myTime = Calendar.getInstance();
            myTime.setTime(dateEnd);
            yearLast = myTime.get(Calendar.YEAR);
        } else {
            String yearmoLast = tableRowList.get(tableRowList.size()-1).getStringColumn(dateColumn);
            yearLast = Integer.valueOf(yearmoLast.split("-")[0]);
        }
        //                    distinctBetween(2011, 2005)  = 7
        int distinctNumberOfYears = yearLast-yearStart+1;

        /**
         * monthlyDataGrid will hold all the years down, and the year number, as well as monthly values, plus total across.
         */
        Integer columns = 1 + 12 + 1 + 1;

        Integer[][] monthlyDataGrid = new Integer[distinctNumberOfYears][columns];

        //Initialize all to zero
        for(int yearIndex = 0; yearIndex < distinctNumberOfYears; yearIndex++) {
            monthlyDataGrid[yearIndex][0] = yearStart+yearIndex;
            for(int dataColumnIndex = 1; dataColumnIndex < columns; dataColumnIndex++) {
                monthlyDataGrid[yearIndex][dataColumnIndex] = 0;
            }
        }



        for(TableRow monthRow: tableRowList) {
            String yearmo = monthRow.getStringColumn(dateColumn);

            String[] yearMonthSplit = yearmo.split("-");
            Integer currentYear = Integer.parseInt(yearMonthSplit[0]);
            Integer currentMonth = Integer.parseInt(yearMonthSplit[1]);

            long monthlyHits = monthRow.getLongColumn(valueColumn);

            monthlyDataGrid[currentYear-yearStart][currentMonth] = (int) monthlyHits;
        }

        // Fill first column with year name. And, fill in last column with cumulative annual total.
        for(int yearIndex = 0; yearIndex < distinctNumberOfYears; yearIndex++) {
            Integer yearCumulative=0;
            for(int monthIndex = 1; monthIndex <= 12; monthIndex++) {
                yearCumulative += monthlyDataGrid[yearIndex][monthIndex];
            }

            monthlyDataGrid[yearIndex][13] = yearCumulative;
            if(yearIndex == 0) {
                monthlyDataGrid[yearIndex][14] = yearCumulative;
            } else {
                monthlyDataGrid[yearIndex][14] = yearCumulative + monthlyDataGrid[yearIndex-1][14];
            }
        }
        return monthlyDataGrid;

    }

    /**
     * Standard conversion of input date, where input is "Month Year", i.e. "December 2011", i.e. "MMMM yyyy".
     * If you need a different date format, then use the other method.
     * @param objectCounts
     * @return
     * @throws ParseException
     */
    public Integer[][] convertObjectCountsToIntegerGrid(ObjectCount[] objectCounts) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        return convertObjectCountsToIntegerGrid(objectCounts, dateFormat);
    }

    public Integer[][] convertObjectCountsToIntegerGrid(ObjectCount[] objectCounts, SimpleDateFormat dateFormat) throws ParseException{

        Calendar calendar = Calendar.getInstance();



        Date date;

        date = dateFormat.parse(objectCounts[0].getValue());
        calendar.setTime(date);
        Integer yearStart = calendar.get(Calendar.YEAR);

        date = dateFormat.parse(objectCounts[objectCounts.length-1].getValue());
        calendar.setTime(date);
        Integer yearLast = calendar.get(Calendar.YEAR);

        int distinctNumberOfYears = yearLast-yearStart+1;

        /**
         * monthlyDataGrid will hold all the years down, and the year number, as well as monthly values, plus total across.
         */
        Integer columns = 1 + 12 + 1 + 1;
        Integer[][] monthlyDataGrid = new Integer[distinctNumberOfYears][columns];

        //Initialize the dataGrid with yearName and blanks
        for(int yearIndex = 0; yearIndex < distinctNumberOfYears; yearIndex++) {
            monthlyDataGrid[yearIndex][0] = yearStart+yearIndex;
            for(int dataColumnIndex = 1; dataColumnIndex < columns; dataColumnIndex++) {
                monthlyDataGrid[yearIndex][dataColumnIndex] = 0;
            }
        }

        //Fill in monthly values
        for(ObjectCount objectCountMonth: objectCounts) {
            date = dateFormat.parse(objectCountMonth.getValue());
            calendar.setTime(date);

            long monthlyHits = objectCountMonth.getCount();
            monthlyDataGrid[calendar.get(Calendar.YEAR)-yearStart][calendar.get(Calendar.MONTH)+1] = (int) monthlyHits;
        }

        // Fill in last column with cumulative annual total.
        for(int yearIndex = 0; yearIndex < distinctNumberOfYears; yearIndex++) {
            Integer yearCumulative=0;
            for(int monthIndex = 1; monthIndex <= 12; monthIndex++) {
                yearCumulative += monthlyDataGrid[yearIndex][monthIndex];
            }

            monthlyDataGrid[yearIndex][13] = yearCumulative;
            if(yearIndex == 0) {
                monthlyDataGrid[yearIndex][14] = yearCumulative;
            } else {
                monthlyDataGrid[yearIndex][14] = yearCumulative + monthlyDataGrid[yearIndex-1][14];
            }
        }
        return monthlyDataGrid;
    }

    public void displayAsGrid(Division division, Integer[][] monthlyDataGrid, String name, String header) throws WingException {
        if(monthlyDataGrid == null || monthlyDataGrid.length == 0) {
            log.error("Grid has no data: "+ header);
            Table gridTable = division.addTable(name, 1,1);
            gridTable.setHead("No Data Available for " + header);
            Row gridHeader = gridTable.addRow(Row.ROLE_HEADER);
            gridHeader.addCell().addContent("Year");
            gridHeader.addCell().addContent("JAN");
            gridHeader.addCell().addContent("FEB");
            gridHeader.addCell().addContent("MAR");
            gridHeader.addCell().addContent("APR");
            gridHeader.addCell().addContent("MAY");
            gridHeader.addCell().addContent("JUN");
            gridHeader.addCell().addContent("JUL");
            gridHeader.addCell().addContent("AUG");
            gridHeader.addCell().addContent("SEP");
            gridHeader.addCell().addContent("OCT");
            gridHeader.addCell().addContent("NOV");
            gridHeader.addCell().addContent("DEC");
            gridHeader.addCell().addContent("Total YR");
            gridHeader.addCell().addContent("Total Cumulative");
            return;
        }

        Integer yearStart = monthlyDataGrid[0][0];
        Integer yearLast = monthlyDataGrid[monthlyDataGrid.length-1][0];
        int numberOfYears = yearLast-yearStart;

        Integer columns = 1 + 12 + 1 + 1;
        Table gridTable = division.addTable(name, numberOfYears+1, columns);
        gridTable.setHead(header);
        Row gridHeader = gridTable.addRow(Row.ROLE_HEADER);
        gridHeader.addCell().addContent("Year");
        gridHeader.addCell().addContent("JAN");
        gridHeader.addCell().addContent("FEB");
        gridHeader.addCell().addContent("MAR");
        gridHeader.addCell().addContent("APR");
        gridHeader.addCell().addContent("MAY");
        gridHeader.addCell().addContent("JUN");
        gridHeader.addCell().addContent("JUL");
        gridHeader.addCell().addContent("AUG");
        gridHeader.addCell().addContent("SEP");
        gridHeader.addCell().addContent("OCT");
        gridHeader.addCell().addContent("NOV");
        gridHeader.addCell().addContent("DEC");
        gridHeader.addCell().addContent("Total YR");
        gridHeader.addCell().addContent("Total Cumulative");

        for(int yearIndex=0; yearIndex < monthlyDataGrid.length; yearIndex++) {
            Row yearRow = gridTable.addRow();
            yearRow.addCell(Cell.ROLE_HEADER).addContent(monthlyDataGrid[yearIndex][0]);
            for(int yearContentIndex = 1; yearContentIndex<columns; yearContentIndex++) {
                yearRow.addCell().addContent(monthlyDataGrid[yearIndex][yearContentIndex]);
            }
        }
    }

    public void displayAsTableRows(Division division, java.util.List<TableRow> tableRowList, String title) throws WingException {
        Table table = division.addTable("itemsInContainer", tableRowList.size()+1, 3);
        table.setHead(title);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("Month");
        header.addCell().addContent("Added During Month");
        header.addCell().addContent("Total Cumulative");

        int cumulativeHits = 0;
        for(TableRow row : tableRowList) {
            Row htmlRow = table.addRow(Row.ROLE_DATA);

            String yearmo = row.getStringColumn("yearmo");
            htmlRow.addCell().addContent(yearmo);

            long monthlyHits = row.getLongColumn("countitem");
            htmlRow.addCell().addContent(""+monthlyHits);

            cumulativeHits += monthlyHits;
            htmlRow.addCell().addContent(""+cumulativeHits);
        }
    }

    public java.util.List<TableRow> addFilesInContainerQuery(DSpaceObject dso) {
        java.util.List<TableRow> tableRowList = null;

        String typeTextLower = "";
        if(dso.getType() == Constants.COMMUNITY) {
            typeTextLower = "communities";
        } else {
            typeTextLower = dso.getTypeText().toLowerCase();
        }

        String querySpecifyContainer = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item, item2bundle, bundle, bundle2bitstream, " +
                typeTextLower + "2item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true AND " +
                "item2bundle.bundle_id = bundle.bundle_id AND item2bundle.item_id = item.item_id AND bundle.bundle_id = bundle2bitstream.bundle_id AND bundle.\"name\" = 'ORIGINAL' AND "+
                typeTextLower + "2item.item_id = item.item_id AND "+
                typeTextLower + "2item."+dso.getTypeText().toLowerCase()+"_id = ? ";

        if (dateStart != null) {
            String start = dateFormat.format(dateStart);
            querySpecifyContainer += "AND metadatavalue.text_value > '"+start+"'";
        }
        if(dateEnd != null) {
            String end = dateFormat.format(dateEnd);
            querySpecifyContainer += " AND metadatavalue.text_value < '"+end+"' ";
        }

        querySpecifyContainer += ") t1 GROUP BY date_trunc('month', t1.ts) order by yearmo asc";

        try {
            TableRowIterator tri = DatabaseManager.query(context, querySpecifyContainer, dso.getID());

            tableRowList = tri.toList();
        } catch (Exception e) {

        }

        return tableRowList;
    }

    public void addFilesInContainer(DSpaceObject dso, Division division) {
        java.util.List<TableRow> tableRowList = addFilesInContainerQuery(dso);

        Gson gson = new Gson();
        try {
            division.addHidden("gson-filesAdded").setValue(gson.toJson(tableRowList));

            Integer[][] monthlyDataGrid = convertTableRowListToIntegerGrid(tableRowList, "yearmo", "countitem");

            displayAsGrid(division, monthlyDataGrid, "filesInContainer-grid", "Number of Files in the "+dso.getName());
            //displayAsTableRows(division, tableRowList, "Number of Files in the "+getTypeAsString(dso));
        } catch (WingException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
