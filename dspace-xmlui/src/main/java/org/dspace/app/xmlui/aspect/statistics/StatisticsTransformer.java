/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public StatisticsTransformer(Date dateStart, Date dateEnd) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

		this.context = new Context();
	}

    public StatisticsTransformer() {
		this.context = new Context();
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
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
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
                if(itemService.hasUploadedFiles(item)){
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

			String[][] matrix = dataset.getMatrix();

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
			headerRow.addCell("spacer", Cell.ROLE_HEADER, "labelcell");

			String[] cLabels = dataset.getColLabels().toArray(new String[0]);
			for (int row = 0; row < cLabels.length; row++) {
				Cell cell = headerRow.addCell(0 + "-" + row + "-h",
                        Cell.ROLE_HEADER, "labelcell");
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

			String[][] matrix = dataset.getMatrix();

			// String[] rLabels = dataset.getRowLabels().toArray(new String[0]);

			Table table = mainDiv.addTable("list-table", matrix.length, 2,
					title == null ? "detailtable" : "tableWithTitle detailtable");
			if (title != null)
            {
                table.setHead(message(title));
            }

			Row headerRow = table.addRow();

			headerRow.addCell("", Cell.ROLE_HEADER, "labelcell");
			
			headerRow.addCell("", Cell.ROLE_HEADER, "labelcell").addContent(message(T_head_visits_views));

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

}
